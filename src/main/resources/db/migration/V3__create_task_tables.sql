CREATE TABLE task_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_custom_added BOOLEAN DEFAULT FALSE
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    job_number VARCHAR(255) NOT NULL UNIQUE,
    task_name VARCHAR(255) NOT NULL,
    template_id UUID REFERENCES task_templates(id),
    category VARCHAR(50) NOT NULL,
    description TEXT,
    project_id UUID,
    assigned_to_id UUID NOT NULL REFERENCES users(id),
    creator_id UUID NOT NULL REFERENCES users(id),
    source VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    planned_start_date DATE,
    planned_end_date DATE,
    planned_efforts_hours DOUBLE PRECISION,
    actual_efforts_hours DOUBLE PRECISION
);

CREATE TABLE task_history (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    changed_by_id UUID NOT NULL REFERENCES users(id),
    remarks TEXT,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexing for performance
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to_id);
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_task_history_task ON task_history(task_id);
