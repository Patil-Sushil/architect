package com.kalibyte.architect.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectStatusHistoryResponse {

    private UUID id;
    private ProjectStatus previousStatus;
    private ProjectStatus newStatus;
    private Integer reworkNumber;
    private String remarks;
    private String changedBy;
    private LocalDateTime changedAt;
}