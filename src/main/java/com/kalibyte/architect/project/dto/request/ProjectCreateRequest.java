package com.kalibyte.architect.project.dto.request;

import com.kalibyte.architect.project.entity.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "Job number is required")
    @Size(max = 50, message = "Job number must not exceed 50 characters")
    private String jobNumber;

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String projectName;

    @NotBlank(message = "Client/Owner name is required")
    @Size(max = 255, message = "Client/Owner name must not exceed 255 characters")
    private String clientOwnerName;

    @NotNull(message = "Project type is required")
    private ProjectType projectType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expected completion date is required")
    private LocalDate expectedCompletionDate;

    // Optional for SMALL projects
    private UUID projectLeadId;

    // Optional employee assignment (especially for SMALL projects)
    private UUID assignedEmployeeId;

    @NotBlank(message = "Site location is required")
    @Size(max = 500, message = "Site location must not exceed 500 characters")
    private String siteLocation;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}