package com.kalibyte.architect.storage.service.impl;

import com.kalibyte.architect.common.exception.BusinessException;
import com.kalibyte.architect.storage.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Local-disk implementation of {@link FileStorageService}.
 *
 * <p>Files are written under a configurable root directory (default: {@code ./uploads}).
 * The relative path returned by {@link #storeFile} encodes the sub-folder so that the
 * database record remains portable — the root can be changed without a data migration.
 *
 * <h3>Security considerations</h3>
 * <ul>
 *   <li><b>Path traversal:</b> {@link StringUtils#cleanPath(String)} strips sequences
 *       like {@code ../} before any I/O takes place.</li>
 *   <li><b>Filename collision:</b> A UUID prefix is prepended to every stored file,
 *       making collisions statistically impossible regardless of the original name.</li>
 *   <li><b>Original name preservation:</b> Only the file <em>extension</em> from the
 *       original filename is reused — the full name is never written to disk.</li>
 * </ul>
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path rootStoragePath;

    /**
     * Constructor injection guarantees the root path is available before any
     * request is processed and makes the dependency explicit for unit testing.
     *
     * @param uploadDir path to the root upload directory, resolved from
     *                  {@code app.storage.upload-dir} (defaults to {@code ./uploads})
     */
    public LocalFileStorageServiceImpl(
            @Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.rootStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Creates the root upload directory (and any missing parents) on application startup.
     * Spring calls this after dependency injection is complete.
     */
    @PostConstruct
    public void initStorage() {
        try {
            Files.createDirectories(rootStoragePath);
            log.info("File storage initialised at: {}", rootStoragePath);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not create file storage directory: " + rootStoragePath, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Storage layout: {@code <root>/<subFolder>/<uuid>.<ext>}
     *
     * @return relative path in the form {@code "<subFolder>/<uuid>.<ext>"}
     */
    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot store an empty or null file.");
        }

        // --- 1. Sanitise the original filename against path-traversal attacks ---
        String originalFilename = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename(),
                        "Uploaded file must have an original filename"));

        if (originalFilename.contains("..")) {
            throw new BusinessException(
                    "Filename contains an illegal path sequence: " + originalFilename);
        }

        // --- 2. Extract extension (empty string if none) ---
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
            extension = "." + originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        // --- 3. Build a UUID-based secure filename ---
        String secureFilename = UUID.randomUUID() + extension;

        // --- 4. Resolve and create the sub-folder under root ---
        Path targetDir = rootStoragePath.resolve(subFolder).normalize();

        // Guard: ensure the resolved path is still inside our root (belt-and-suspenders)
        if (!targetDir.startsWith(rootStoragePath)) {
            throw new BusinessException(
                    "Cannot store file outside of the designated upload directory.");
        }

        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            throw new BusinessException(
                    "Could not create storage sub-folder '" + targetDir + "': " + ex.getMessage());
        }

        // --- 5. Write to disk atomically via InputStream copy ---
        Path targetFile = targetDir.resolve(secureFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(
                    "Failed to store file '" + originalFilename + "': " + ex.getMessage());
        }

        // --- 6. Return the portable relative path ---
        String relativePath = subFolder + "/" + secureFilename;
        log.info("Stored file '{}' as '{}' (size: {} bytes)",
                originalFilename, relativePath, targetFile.toFile().length());
        return relativePath;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the relative path against the root storage directory and returns a
     * {@link UrlResource} that Spring MVC can stream directly into the HTTP response
     * via {@code ResponseEntity<Resource>}.
     */
    @Override
    public Resource loadFile(String filePath) {
        Path target = rootStoragePath.resolve(filePath).normalize();

        // Guard: prevent traversal outside root
        if (!target.startsWith(rootStoragePath)) {
            throw new com.kalibyte.architect.common.exception.ResourceNotFoundException(
                    "File not accessible: " + filePath);
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new com.kalibyte.architect.common.exception.ResourceNotFoundException(
                        "File not found or not readable: " + filePath);
            }
            log.debug("Serving file: {}", target);
            return resource;
        } catch (java.net.MalformedURLException ex) {
            throw new com.kalibyte.architect.common.exception.ResourceNotFoundException(
                    "Could not resolve file path: " + filePath);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Non-existent files are logged as a warning but do not raise an exception,
     * keeping delete operations idempotent.
     */
    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            log.warn("deleteFile called with a blank path — skipping.");
            return;
        }

        Path target = rootStoragePath.resolve(filePath).normalize();

        // Guard: prevent deletion of files outside our root
        if (!target.startsWith(rootStoragePath)) {
            throw new BusinessException(
                    "Refusing to delete a file outside the designated upload directory: " + filePath);
        }

        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("Deleted stored file: {}", target);
            } else {
                log.warn("File not found during delete (already removed?): {}", target);
            }
        } catch (IOException ex) {
            // Log but do not rethrow — callers (e.g. orphan-cleanup) should remain resilient
            log.error("Failed to delete file '{}': {}", target, ex.getMessage(), ex);
        }
    }
}
