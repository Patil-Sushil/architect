package com.kalibyte.architect.project.mapper;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.project.dto.request.ProjectCreateRequest;
import com.kalibyte.architect.project.dto.request.ProjectUpdateRequest;
import com.kalibyte.architect.project.dto.response.AssignableUserResponse;
import com.kalibyte.architect.project.dto.response.ProjectDeleteResponse;
import com.kalibyte.architect.project.dto.response.ProjectResponse;
import com.kalibyte.architect.project.dto.response.ProjectStatusHistoryResponse;
import com.kalibyte.architect.project.entity.Project;
import com.kalibyte.architect.project.entity.ProjectStatusHistory;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProjectMapper {

    @Mapping(target = "projectLead",
            source = "projectLead",
            qualifiedByName = "userToLeadSummary")
    @Mapping(target = "assignedEmployee",
            source = "assignedEmployee",
            qualifiedByName = "userToEmployeeSummary")
    ProjectResponse toResponse(Project project);

    List<ProjectResponse> toResponseList(List<Project> projects);

    // Map Project to Delete Response
    @Mapping(target = "projectId", source = "id")
    ProjectDeleteResponse toDeleteResponse(Project project);

    @Mapping(target = "id",                  ignore = true)
    @Mapping(target = "status",              ignore = true)
    @Mapping(target = "reworkCount",         ignore = true)
    @Mapping(target = "deleted",             ignore = true)
    @Mapping(target = "deletedAt",           ignore = true)
    @Mapping(target = "deletedBy",           ignore = true)
    @Mapping(target = "projectLead",         ignore = true)
    @Mapping(target = "assignedEmployee",    ignore = true)
    @Mapping(target = "statusHistory",       ignore = true)
    @Mapping(target = "createdAt",           ignore = true)
    @Mapping(target = "updatedAt",           ignore = true)
    @Mapping(target = "createdBy",           ignore = true)
    @Mapping(target = "updatedBy",           ignore = true)
    @Mapping(target = "actualCompletionDate",ignore = true)
    Project toEntity(ProjectCreateRequest request);

    @Mapping(target = "id",                  ignore = true)
    @Mapping(target = "jobNumber",           ignore = true)
    @Mapping(target = "status",              ignore = true)
    @Mapping(target = "reworkCount",         ignore = true)
    @Mapping(target = "deleted",             ignore = true)
    @Mapping(target = "deletedAt",           ignore = true)
    @Mapping(target = "deletedBy",           ignore = true)
    @Mapping(target = "projectLead",         ignore = true)
    @Mapping(target = "assignedEmployee",    ignore = true)
    @Mapping(target = "statusHistory",       ignore = true)
    @Mapping(target = "createdAt",           ignore = true)
    @Mapping(target = "updatedAt",           ignore = true)
    @Mapping(target = "createdBy",           ignore = true)
    @Mapping(target = "updatedBy",           ignore = true)
    void updateEntityFromRequest(ProjectUpdateRequest request,
                                 @MappingTarget Project project);

    ProjectStatusHistoryResponse toHistoryResponse(ProjectStatusHistory history);

    List<ProjectStatusHistoryResponse> toHistoryResponseList(
            List<ProjectStatusHistory> historyList);

    AssignableUserResponse toAssignableUserResponse(User user);

    List<AssignableUserResponse> toAssignableUserResponseList(List<User> users);

    @Named("userToLeadSummary")
    default ProjectResponse.ProjectLeadSummary userToLeadSummary(User user) {
        if (user == null) {
            return null;
        }
        ProjectResponse.ProjectLeadSummary summary =
                new ProjectResponse.ProjectLeadSummary();
        summary.setId(user.getId());
        summary.setName(user.getName());
        summary.setEmail(user.getEmail());
        summary.setPhone(user.getPhone());
        return summary;
    }

    @Named("userToEmployeeSummary")
    default ProjectResponse.EmployeeSummary userToEmployeeSummary(User user) {
        if (user == null) {
            return null;
        }
        ProjectResponse.EmployeeSummary summary =
                new ProjectResponse.EmployeeSummary();
        summary.setId(user.getId());
        summary.setName(user.getName());
        summary.setEmail(user.getEmail());
        summary.setPhone(user.getPhone());
        return summary;
    }
}