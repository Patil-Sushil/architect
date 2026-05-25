package com.kalibyte.architect.task.dto.request;

import com.kalibyte.architect.task.entity.enums.TaskCategory;
import com.kalibyte.architect.task.entity.enums.TaskPriority;
import com.kalibyte.architect.task.entity.enums.TaskReferenceType;
import com.kalibyte.architect.task.entity.enums.TaskSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TaskCreateRequest {
    
    private String jobNumber;
    
    private UUID projectId;
    
    @NotBlank(message = "Task name is required")
    private String taskName;
    
    private UUID templateId;
    
    @NotNull(message = "Category is required")
    private TaskCategory category;
    
    private String description;
    
    @NotNull(message = "Assigned user ID is required")
    private UUID assignedToUserId;
    
    private TaskSource source;
    
    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    private TaskReferenceType referenceType;

    private String referredBy;
    
    private LocalDate plannedStartDate;
    
    private LocalDate plannedEndDate;
    
    private Double plannedEffortsHours;
}
