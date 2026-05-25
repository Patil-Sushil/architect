package com.kalibyte.architect.project.entity;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.common.base.BaseEntity;
import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_job_number", columnList = "job_number"),
        @Index(name = "idx_project_status", columnList = "status"),
        @Index(name = "idx_project_type", columnList = "project_type"),
        @Index(name = "idx_project_deleted", columnList = "deleted")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

    @Column(name = "job_number", nullable = false, unique = true, length = 50)
    private String jobNumber;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "client_owner_name", nullable = false, length = 255)
    private String clientOwnerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 20)
    private ProjectType projectType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expected_completion_date", nullable = false)
    private LocalDate expectedCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_lead_id")
    private User projectLead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private User assignedEmployee;

    @Column(name = "site_location", nullable = false, length = 500)
    private String siteLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.PLANNING;

    @Column(name = "rework_count", nullable = false)
    private Integer reworkCount = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("changedAt DESC")
    private List<ProjectStatusHistory> statusHistory = new ArrayList<>();

    // ============================================================
    // BUSINESS METHODS
    // ============================================================

    /**
     * Soft delete with audit trail
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * Restore soft-deleted project
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    /**
     * Increment rework counter
     */
    public void incrementRework() {
        this.reworkCount++;
    }

    /**
     * Check if project is completed
     */
    public boolean isCompleted() {
        return this.status == ProjectStatus.COMPLETED;
    }

    /**
     * Check if project is in rework
     */
    public boolean isInRework() {
        return this.status == ProjectStatus.REWORK;
    }

    /**
     * Check if project is active
     */
    public boolean isActive() {
        return !this.deleted && this.status != ProjectStatus.CANCELLED;
    }

    /**
     * Get project duration in days
     */
    public long getProjectDurationDays() {
        if (actualCompletionDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, actualCompletionDate);
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
    }

    /**
     * Check if project is overdue
     */
    public boolean isOverdue() {
        return this.status != ProjectStatus.COMPLETED
                && this.status != ProjectStatus.CANCELLED
                && LocalDate.now().isAfter(this.expectedCompletionDate);
    }
}