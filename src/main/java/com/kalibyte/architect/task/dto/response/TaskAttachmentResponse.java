package com.kalibyte.architect.task.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@code TaskAttachment} returned inside {@link TaskResponse}.
 * Intentionally excludes the raw {@code filePath} (server-internal) — clients
 * always fetch the file via the dedicated download endpoint instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachmentResponse {

    private UUID id;

    /** Human-readable original filename, e.g. "ground_floor_layout.dwg" */
    private String fileName;

    /** Lowercase file extension derived from the original filename, e.g. "dwg", "pdf", "rvt" */
    private String fileType;

    /** When the attachment was uploaded */
    private LocalDateTime uploadedAt;

    /** Who uploaded it (display name) */
    private String uploadedBy;

    /**
     * Ready-to-use download URL the front-end can embed in an anchor tag.
     * Pattern: {@code /api/tasks/{taskId}/attachments/{attachmentId}/download}
     */
    private String downloadUrl;
}
