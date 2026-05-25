package com.kalibyte.architect.project.dto.request;


import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import lombok.Data;

import java.util.UUID;

@Data
public class ProjectFilterRequest {
    private String search;
    private ProjectStatus status;
    private ProjectType projectType;
    private UUID projectLeadId;
    private UUID assignedEmployeeId;
}
