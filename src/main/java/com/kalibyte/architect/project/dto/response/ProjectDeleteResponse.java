package com.kalibyte.architect.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectDeleteResponse {
    private UUID projectId;
    private String jobNumber;
    private String projectName;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String message;
}
