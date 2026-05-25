package com.kalibyte.architect.task.entity;

import com.kalibyte.architect.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents a file attachment associated with a submitted task.
 * Stores metadata about the file; actual binary content lives on disk (or future cloud storage).
 */
@Entity
@Table(name = "task_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TaskAttachment extends BaseEntity {

    /**
     * The task this attachment belongs to.
     * Loaded lazily to avoid unnecessary joins when only task-level data is needed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The original, human-readable filename as uploaded by the employee.
     * e.g. "ground_floor_layout.dwg"
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * The relative path on the storage back-end where the file is persisted.
     * e.g. "tasks/3f2e1d0c-uuid-here.dwg"
     * Kept relative so the root storage location can change (local → cloud) without a DB migration.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * The lowercase file-extension / MIME category derived from the original filename.
     * e.g. "dwg", "pdf", "rvt"
     * Nullable – a file with no extension is still accepted.
     */
    @Column(name = "file_type")
    private String fileType;
}
