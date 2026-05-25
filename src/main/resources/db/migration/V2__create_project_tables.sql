-- ============================================================
-- V2: Project Management Module
-- ============================================================

CREATE TABLE projects (
                          id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                          job_number               VARCHAR(50)  NOT NULL UNIQUE,
                          project_name             VARCHAR(255) NOT NULL,
                          client_owner_name        VARCHAR(255) NOT NULL,
                          project_type             VARCHAR(20)  NOT NULL,
                          start_date               DATE         NOT NULL,
                          expected_completion_date DATE         NOT NULL,
                          actual_completion_date   DATE,
                          project_lead_id          UUID,
                          assigned_employee_id     UUID,
                          site_location            VARCHAR(500) NOT NULL,
                          status                   VARCHAR(30)  NOT NULL DEFAULT 'PLANNING',
                          rework_count             INTEGER      NOT NULL DEFAULT 0,
                          description              TEXT,
                          deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
                          deleted_at               TIMESTAMP,
                          deleted_by               VARCHAR(255),
                          created_at               TIMESTAMP,
                          updated_at               TIMESTAMP,
                          created_by               VARCHAR(255),
                          updated_by               VARCHAR(255),

                          CONSTRAINT fk_project_lead
                              FOREIGN KEY (project_lead_id)
                                  REFERENCES users(id)
                                  ON DELETE RESTRICT,

                          CONSTRAINT fk_project_assigned_employee
                              FOREIGN KEY (assigned_employee_id)
                                  REFERENCES users(id)
                                  ON DELETE RESTRICT
);

CREATE INDEX idx_projects_status              ON projects(status);
CREATE INDEX idx_projects_project_type        ON projects(project_type);
CREATE INDEX idx_projects_project_lead_id     ON projects(project_lead_id);
CREATE INDEX idx_projects_assigned_employee_id ON projects(assigned_employee_id);
CREATE INDEX idx_projects_deleted             ON projects(deleted);
CREATE INDEX idx_projects_job_number          ON projects(job_number);
CREATE INDEX idx_projects_start_date          ON projects(start_date);

CREATE TABLE project_status_history (
                                        id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                        project_id      UUID        NOT NULL,
                                        previous_status VARCHAR(30),
                                        new_status      VARCHAR(30) NOT NULL,
                                        rework_number   INTEGER,
                                        remarks         TEXT,
                                        changed_by      VARCHAR(255),
                                        changed_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_status_history_project
                                            FOREIGN KEY (project_id)
                                                REFERENCES projects(id)
                                                ON DELETE CASCADE
);

CREATE INDEX idx_status_history_project_id ON project_status_history(project_id);
CREATE INDEX idx_status_history_changed_at ON project_status_history(changed_at);

CREATE TABLE attendance (
                            id UUID PRIMARY KEY,
                            user_id UUID NOT NULL,
                            date DATE NOT NULL,
                            login_time TIMESTAMP NOT NULL,
                            logout_time TIMESTAMP,
                            total_hours DOUBLE PRECISION,
                            exception_note VARCHAR(255),
                            is_manually_edited BOOLEAN DEFAULT FALSE,
                            CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX idx_attendance_user_date ON attendance(user_id, date);