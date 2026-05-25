package com.kalibyte.architect.project.dto.request;


import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectStatusUpdateRequest {

    @NotNull(message = "New status is required")
    private ProjectStatus newStatus;

    @Size(max = 2000, message = "Remarks must not exceed 2000 characters")
    private String remarks;
}
