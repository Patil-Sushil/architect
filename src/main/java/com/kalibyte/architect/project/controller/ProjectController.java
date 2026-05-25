package com.kalibyte.architect.project.controller;

import com.kalibyte.architect.audit.entity.enums.AuditAction;
import com.kalibyte.architect.common.annotation.LoggableAction;
import com.kalibyte.architect.common.response.ApiResponse;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.project.dto.request.ProjectCreateRequest;
import com.kalibyte.architect.project.dto.request.ProjectFilterRequest;
import com.kalibyte.architect.project.dto.request.ProjectStatusUpdateRequest;
import com.kalibyte.architect.project.dto.request.ProjectUpdateRequest;
import com.kalibyte.architect.project.dto.response.AssignableUserResponse;
import com.kalibyte.architect.project.dto.response.ProjectDeleteResponse;
import com.kalibyte.architect.project.dto.response.ProjectResponse;
import com.kalibyte.architect.project.dto.response.ProjectStatusHistoryResponse;
import com.kalibyte.architect.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Create a new project",
            action = AuditAction.PROJECT_CREATED,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectCreateRequest request) {

        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Update an existing project details",
            action = AuditAction.PROJECT_UPDATED,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequest request) {

        ProjectResponse response = projectService.updateProject(id, request);
        return ResponseEntity.ok(ApiResponse.success("Project updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Delete a project",
            action = AuditAction.PROJECT_DELETED,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<ProjectDeleteResponse>> deleteProject(@PathVariable UUID id) {
        ProjectDeleteResponse response = projectService.deleteProject(id);
        return ResponseEntity.ok(
                ApiResponse.success("Project deleted successfully", response)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Retrieve a single project by ID",
            action = AuditAction.OTHER,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Retrieve a paginated and filtered list of projects",
            action = AuditAction.GET_ALL_PROJECTS,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> getAllProjects(
            @ModelAttribute ProjectFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                ApiResponse.success(projectService.getAllProjects(filter, pageable))
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Update project status",
            action = AuditAction.PROJECT_STATUS_CHANGED,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProjectStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectStatusUpdateRequest request) {

        ProjectResponse response = projectService.updateProjectStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Project status updated successfully", response));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Fetch status transition history for a project",
            action = AuditAction.GET_STATUS_HISTORY,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<List<ProjectStatusHistoryResponse>>> getStatusHistory(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectStatusHistory(id)));
    }

    @GetMapping("/managers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Retrieve a list of all assignable project managers",
            action = AuditAction.GET_ALL_PROJECT_MANAGERS,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<List<AssignableUserResponse>>> getAllProjectManagers() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjectManagers()));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Retrieve a list of all assignable employees",
            action = AuditAction.GET_ALL_PROJECT_EMPLOYEES,
            entityType = "PROJECT"
    )
    public ResponseEntity<ApiResponse<List<AssignableUserResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllEmployees()));
    }
}