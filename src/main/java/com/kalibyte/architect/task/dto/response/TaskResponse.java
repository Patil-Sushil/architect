package com.kalibyte.architect.task.dto.response;

import com.kalibyte.architect.task.entity.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private String jobNumber;
    private String taskName;
    private TaskTemplateResponse template;
    private TaskCategory category;
    private String description;
    private UUID projectId;
    private String projectName;
    private String projectJobNumber;
    private UserSummary assignedTo;
    private UserSummary createdBy;
    private TaskSource source;
    private TaskPriority priority;
    private TaskStatus status;
    private TaskReferenceType referenceType;
    private String referredBy;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private Double plannedEffortsHours;
    private Double actualEffortsHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Files uploaded when the employee submitted this task. Empty list if no files were attached. */
    private List<TaskAttachmentResponse> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private UUID id;
        private String name;
        private String email;
    }
}
