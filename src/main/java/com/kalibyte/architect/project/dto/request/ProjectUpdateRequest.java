package com.kalibyte.architect.project.dto.request;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProjectUpdateRequest {

    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String projectName;

    @Size(max = 255, message = "Client/Owner name must not exceed 255 characters")
    private String clientOwnerName;

    private ProjectType projectType;

    private LocalDate startDate;

    private LocalDate expectedCompletionDate;

    private LocalDate actualCompletionDate;

    private UUID projectLeadId;

    private UUID assignedEmployeeId;

    @Size(max = 500, message = "Site location must not exceed 500 characters")
    private String siteLocation;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}