-- ============================================================
-- V4: Add Reference Fields to Tasks Table
-- ============================================================

ALTER TABLE tasks ADD COLUMN reference_type VARCHAR(50);
ALTER TABLE tasks ADD COLUMN referred_by VARCHAR(255);

CREATE INDEX idx_tasks_reference_type ON tasks(reference_type);
CREATE INDEX idx_tasks_referred_by ON tasks(referred_by);
