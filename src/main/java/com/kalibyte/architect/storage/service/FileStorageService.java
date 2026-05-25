package com.kalibyte.architect.storage.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for binary file assets (CAD drawings, Revit files, PDFs, etc.).
 *
 * <p>The interface is the only coupling point between the application layer and the
 * physical storage back-end. Swapping from local disk to S3/Azure Blob/GCS requires
 * nothing more than providing a new {@code @Primary} implementation bean — zero changes
 * to controllers, services, or domain entities.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Generating secure, collision-free file names.</li>
 *   <li>Preventing path-traversal attacks.</li>
 *   <li>Returning a stable, relative path that can be stored in {@code TaskAttachment.filePath}.</li>
 * </ul>
 */
public interface FileStorageService {

    /**
     * Persists a multipart file in the given logical sub-folder and returns the
     * relative storage path that should be recorded in the database.
     *
     * @param file      the incoming multipart file (must not be {@code null} or empty)
     * @param subFolder logical sub-folder inside the root storage location, e.g. {@code "tasks"}
     * @return a relative path such as {@code "tasks/3f2e1d0c-uuid.dwg"} — never {@code null}
     * @throws com.kalibyte.architect.common.exception.BusinessException if the file cannot be stored
     */
    String storeFile(MultipartFile file, String subFolder);

    /**
     * Resolves the file identified by {@code filePath} and returns it as a Spring
     * {@link Resource} ready to be streamed in an HTTP response body.
     *
     * @param filePath the relative path previously returned by {@link #storeFile}
     * @return a readable, URL-accessible {@link Resource}
     * @throws com.kalibyte.architect.common.exception.ResourceNotFoundException
     *         if no file exists at the given path
     */
    Resource loadFile(String filePath);

    /**
     * Deletes the file identified by the given relative path from the storage back-end.
     *
     * <p>Implementations should log a warning rather than throw if the file does not exist,
     * so that orphan-cleanup jobs remain idempotent.
     *
     * @param filePath the relative path previously returned by {@link #storeFile}
     */
    void deleteFile(String filePath);
}
