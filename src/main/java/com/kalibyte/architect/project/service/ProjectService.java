package com.kalibyte.architect.project.service;

import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.project.dto.request.ProjectCreateRequest;
import com.kalibyte.architect.project.dto.request.ProjectFilterRequest;
import com.kalibyte.architect.project.dto.request.ProjectStatusUpdateRequest;
import com.kalibyte.architect.project.dto.request.ProjectUpdateRequest;
import com.kalibyte.architect.project.dto.response.AssignableUserResponse;
import com.kalibyte.architect.project.dto.response.ProjectDeleteResponse;
import com.kalibyte.architect.project.dto.response.ProjectResponse;
import com.kalibyte.architect.project.dto.response.ProjectStatusHistoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse createProject(ProjectCreateRequest request);

    ProjectResponse updateProject(UUID id, ProjectUpdateRequest request);

    ProjectDeleteResponse deleteProject(UUID id);

    ProjectResponse getProjectById(UUID id);

    PageResponse<ProjectResponse> getAllProjects(ProjectFilterRequest filter,
                                                 Pageable pageable);

    ProjectResponse updateProjectStatus(UUID id, ProjectStatusUpdateRequest request);

    List<ProjectStatusHistoryResponse> getProjectStatusHistory(UUID id);

    List<AssignableUserResponse> getAllProjectManagers();

    List<AssignableUserResponse> getAllEmployees();
}