package com.kalibyte.architect.audit.entity.enums;

public enum AuditAction {

    // ── Authentication ──────────────────────────────────────
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,
    PASSWORD_CHANGED,
    TOKEN_REFRESHED,

    // ── User Management ──────────────────────────────────────
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_ENABLED,
    USER_DISABLED,
    ROLE_ASSIGNED,

    // ── Project Management ───────────────────────────────────
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    PROJECT_STATUS_CHANGED,
    PROJECT_REWORK_STARTED,
    GET_ALL_PROJECTS,
    GET_STATUS_HISTORY,
    GET_ALL_PROJECT_MANAGERS,
    GET_ALL_PROJECT_EMPLOYEES,


    // ── Data Access ──────────────────────────────────────────
    DATA_EXPORTED,

    // ── Security ────────────────────────────────────────────
    UNAUTHORIZED_ACCESS,
    PERMISSION_DENIED,

    //  ── LOG ────────────────────────────────────────────────
    GET_TOP_ACTIVE_USERS,
    EXPORT_AUDIT_LOGS,


    // ── Other ────────────────────────────────────────────────
    OTHER;

    public String getDisplayName() {
        return this.name().replace('_', ' ');
    }
}
