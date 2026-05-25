-- V5: Add task_attachments table to persist file metadata for submitted tasks.
-- The actual binary files are stored on disk (or future cloud storage);
-- this table only tracks the metadata required to locate and describe them.

CREATE TABLE task_attachments (
    id              UUID        PRIMARY KEY,
    created_at      TIMESTAMP   WITHOUT TIME ZONE,
    updated_at      TIMESTAMP   WITHOUT TIME ZONE,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    task_id         UUID        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    file_name       VARCHAR(512) NOT NULL,
    file_path       VARCHAR(1024) NOT NULL,
    file_type       VARCHAR(50)
);

-- Index for fast retrieval of all attachments belonging to a task
CREATE INDEX idx_task_attachments_task ON task_attachments(task_id);
