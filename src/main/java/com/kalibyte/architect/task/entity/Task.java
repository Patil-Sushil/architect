package com.kalibyte.architect.task.entity;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.common.base.BaseEntity;
import com.kalibyte.architect.project.entity.Project;
import com.kalibyte.architect.task.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Task extends BaseEntity {

    @Column(name = "job_number", nullable = false, unique = true)
    private String jobNumber;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TaskTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to_id", nullable = false)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User taskCreator; // Renamed to avoid conflict with BaseEntity.createdBy

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskReferenceType referenceType;

    private String referredBy;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private Double plannedEffortsHours;
    private Double actualEffortsHours;

    /**
     * Files attached when an employee submits this task for review.
     * Cascade ALL + orphanRemoval means the collection is the single source of truth:
     * adding/removing an element here is enough — no extra repository calls needed.
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAttachment> attachments = new ArrayList<>();
}
