-- ============================================================
-- V1: Full Database Initialization
-- Auth, Users, Roles, Enhanced Audit Log, Tokens, Skills
-- Target DB: PostgreSQL
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- AUTH: role table
-- ============================================================
CREATE TABLE role (
                      id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                      name        VARCHAR(50)  NOT NULL UNIQUE,
                      description VARCHAR(255)
);

-- ============================================================
-- AUTH: users table
-- ============================================================
CREATE TABLE users (
                       id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       name                VARCHAR(255) NOT NULL,
                       email               VARCHAR(255) NOT NULL UNIQUE,
                       password            VARCHAR(255) NOT NULL,
                       phone               VARCHAR(255) NOT NULL,
                       date_of_joining     DATE,
                       bank_account_number VARCHAR(255),
                       bank_ifsc           VARCHAR(255),
                       address             VARCHAR(255),
                       enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
                       deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at          TIMESTAMP
);

-- ============================================================
-- AUTH: user_role join table
-- ============================================================
CREATE TABLE user_role (
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           role_id UUID NOT NULL REFERENCES role(id)  ON DELETE CASCADE,
                           PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- AUTH: enhanced audit_log table
-- ============================================================
CREATE TABLE audit_log (
                           id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id             UUID,
                           username            VARCHAR(255),
                           action              VARCHAR(50)  NOT NULL,
                           action_description  VARCHAR(500),
                           entity_type         VARCHAR(100),
                           entity_id           UUID,
                           entity_name         VARCHAR(255),
                           ip_address          VARCHAR(45),
                           user_agent          VARCHAR(500),
                           status              VARCHAR(20)  DEFAULT 'SUCCESS',
                           error_message       TEXT,
                           old_value           TEXT,
                           new_value           TEXT,
                           timestamp           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           duration_ms         BIGINT,

                           CONSTRAINT fk_audit_log_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE SET NULL
);

-- ============================================================
-- audit_log indexes
-- ============================================================
CREATE INDEX idx_audit_log_user_id      ON audit_log(user_id);
CREATE INDEX idx_audit_log_username     ON audit_log(username);
CREATE INDEX idx_audit_log_action       ON audit_log(action);
CREATE INDEX idx_audit_log_timestamp    ON audit_log(timestamp DESC);
CREATE INDEX idx_audit_log_entity_type  ON audit_log(entity_type);
CREATE INDEX idx_audit_log_entity_id    ON audit_log(entity_id);
CREATE INDEX idx_audit_log_status       ON audit_log(status);
CREATE INDEX idx_audit_log_ip_address   ON audit_log(ip_address);
CREATE INDEX idx_audit_log_user_timestamp
    ON audit_log(user_id, timestamp DESC);
CREATE INDEX idx_audit_log_entity_timestamp
    ON audit_log(entity_type, entity_id, timestamp DESC);

-- ============================================================
-- AUTH: refresh_tokens table
-- ============================================================
CREATE TABLE refresh_tokens (
                                id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id     UUID      NOT NULL,
                                token       TEXT      NOT NULL UNIQUE,
                                expiry_date TIMESTAMP NOT NULL,
                                revoked     BOOLEAN   DEFAULT FALSE,
                                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);

-- ============================================================
-- USER SKILLS table
-- ============================================================
CREATE TABLE user_skills (
                             user_id UUID        NOT NULL,
                             skill   VARCHAR(100) NOT NULL,
                             PRIMARY KEY (user_id, skill),
                             CONSTRAINT fk_user_skills_user
                                 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_skills_skill ON user_skills(skill);

-- ============================================================
-- SEED DATA: Roles
-- ============================================================
INSERT INTO role (name, description) VALUES
                                         ('ADMIN',           'Administrator with full access'),
                                         ('HR',              'Human Resources'),
                                         ('PROJECT_MANAGER', 'Project Manager'),
                                         ('EMPLOYEE',        'Regular Employee'),
                                         ('SITE_PERSON',     'Site Personnel');