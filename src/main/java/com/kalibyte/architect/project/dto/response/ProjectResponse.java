package com.kalibyte.architect.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectResponse {

    private UUID id;
    private String jobNumber;
    private String projectName;
    private String clientOwnerName;
    private ProjectType projectType;
    private LocalDate startDate;
    private LocalDate expectedCompletionDate;
    private LocalDate actualCompletionDate;
    private ProjectLeadSummary projectLead;
    private EmployeeSummary assignedEmployee;
    private String siteLocation;
    private ProjectStatus status;
    private Integer reworkCount;
    private String description;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // ✅ NEW: Soft delete audit fields
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProjectLeadSummary {
        private UUID id;
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmployeeSummary {
        private UUID id;
        private String name;
        private String email;
        private String phone;
    }
}