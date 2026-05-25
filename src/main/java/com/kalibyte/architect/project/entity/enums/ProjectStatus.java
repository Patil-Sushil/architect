package com.kalibyte.architect.project.entity.enums;

public enum ProjectStatus {
    PLANNING,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    REWORK,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(ProjectStatus next) {
        return switch (this) {
            case PLANNING    -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == ON_HOLD || next == COMPLETED || next == CANCELLED;
            case ON_HOLD     -> next == IN_PROGRESS || next == CANCELLED;
            case COMPLETED   -> next == REWORK;
            case REWORK      -> next == IN_PROGRESS || next == COMPLETED || next == CANCELLED;
            case CANCELLED   -> false;
        };
    }
}