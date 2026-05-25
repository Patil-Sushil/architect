package com.kalibyte.architect.project.service.impl;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.entity.enums.RoleName;
import com.kalibyte.architect.auth.repository.UserRepository;
import com.kalibyte.architect.common.exception.BusinessException;
import com.kalibyte.architect.common.exception.ResourceNotFoundException;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.project.dto.request.ProjectCreateRequest;
import com.kalibyte.architect.project.dto.request.ProjectFilterRequest;
import com.kalibyte.architect.project.dto.request.ProjectStatusUpdateRequest;
import com.kalibyte.architect.project.dto.request.ProjectUpdateRequest;
import com.kalibyte.architect.project.dto.response.AssignableUserResponse;
import com.kalibyte.architect.project.dto.response.ProjectDeleteResponse;
import com.kalibyte.architect.project.dto.response.ProjectResponse;
import com.kalibyte.architect.project.dto.response.ProjectStatusHistoryResponse;
import com.kalibyte.architect.project.entity.Project;
import com.kalibyte.architect.project.entity.ProjectStatusHistory;
import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import com.kalibyte.architect.project.mapper.ProjectMapper;
import com.kalibyte.architect.project.repository.ProjectRepository;
import com.kalibyte.architect.project.repository.ProjectStatusHistoryRepository;
import com.kalibyte.architect.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        log.info("Creating project with job number: {}", request.getJobNumber());

        // Validate unique job number
        if (projectRepository.existsByJobNumber(request.getJobNumber())) {
            throw new BusinessException(
                    "Project with job number '" + request.getJobNumber() + "' already exists");
        }

        // Validate date range
        validateDateRange(request.getStartDate(), request.getExpectedCompletionDate());

        // Map basic fields
        Project project = projectMapper.toEntity(request);

        // Apply assignment rules
        applyCreateAssignments(project, request);

        // Set defaults
        project.setStatus(ProjectStatus.PLANNING);
        project.setReworkCount(0);
        project.setDeleted(false);

        // Save
        Project saved = projectRepository.save(project);

        // Record initial status
        recordStatusChange(saved, null, ProjectStatus.PLANNING, null,
                "Project created", currentUsername());

        log.info("Project created: {} | Job: {}", saved.getId(), saved.getJobNumber());
        return projectMapper.toResponse(saved);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectUpdateRequest request) {
        log.info("Updating project: {}", id);

        Project project = findActiveProject(id);

        // Validate effective date range
        LocalDate effectiveStart = request.getStartDate() != null
                ? request.getStartDate() : project.getStartDate();
        LocalDate effectiveEnd = request.getExpectedCompletionDate() != null
                ? request.getExpectedCompletionDate() : project.getExpectedCompletionDate();

        validateDateRange(effectiveStart, effectiveEnd);

        // Apply partial updates
        projectMapper.updateEntityFromRequest(request, project);

        // Apply assignment rules
        applyUpdateAssignments(project, request);

        // Save
        Project saved = projectRepository.save(project);
        log.info("Project updated: {}", saved.getId());
        return projectMapper.toResponse(saved);
    }

    // ============================================================
    // DELETE (SOFT DELETE WITH AUDIT)
    // ============================================================

    @Override
    @Transactional
    public ProjectDeleteResponse deleteProject(UUID id) {
        log.info("Soft deleting project: {}", id);

        Project project = findActiveProject(id);

        // Business rule: Cannot delete in-progress projects
        if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "Cannot delete a project that is currently IN_PROGRESS. " +
                            "Please change status first.");
        }

        // Get current user
        String deletedBy = currentUsername();

        // Perform soft delete
        project.softDelete(deletedBy);

        // Save to persist deletedAt and deletedBy
        Project deleted = projectRepository.save(project);

        log.warn("⚠️ Project soft-deleted: {} | Job: {} | By: {} | At: {}",
                deleted.getId(),
                deleted.getJobNumber(),
                deleted.getDeletedBy(),
                deleted.getDeletedAt());

        // Return detailed delete response
        return ProjectDeleteResponse.builder()
                .projectId(deleted.getId())
                .jobNumber(deleted.getJobNumber())
                .projectName(deleted.getProjectName())
                .deletedAt(deleted.getDeletedAt())
                .deletedBy(deleted.getDeletedBy())
                .message("Project '" + deleted.getJobNumber() + "' has been successfully deleted")
                .build();
    }

    // ============================================================
    // READ
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + id));
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAllProjects(
            ProjectFilterRequest filter, Pageable pageable) {

        // Normalize search term
        String search = filter.getSearch() != null && !filter.getSearch().isBlank()
                ? filter.getSearch().trim()
                : null;

        Page<Project> page = projectRepository.findAllWithFilters(
                filter.getStatus(),
                filter.getProjectType(),
                filter.getProjectLeadId(),
                filter.getAssignedEmployeeId(),
                search,
                pageable
        );

        return PageResponse.from(page, projectMapper::toResponse);
    }

    // ============================================================
    // STATUS MANAGEMENT
    // ============================================================

    @Override
    @Transactional
    public ProjectResponse updateProjectStatus(UUID id, ProjectStatusUpdateRequest request) {
        log.info("Updating project status: {} → {}", id, request.getNewStatus());

        Project project = findActiveProject(id);
        ProjectStatus currentStatus = project.getStatus();
        ProjectStatus newStatus = request.getNewStatus();

        // Validate transition
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException(
                    String.format("Invalid status transition: %s → %s",
                            currentStatus, newStatus));
        }

        Integer reworkNumber = null;

        // Handle REWORK
        if (newStatus == ProjectStatus.REWORK) {
            project.incrementRework();
            reworkNumber = project.getReworkCount();
            log.info("Project {} entering rework cycle #{}", id, reworkNumber);
        }

        // Auto-set completion date
        if (newStatus == ProjectStatus.COMPLETED && project.getActualCompletionDate() == null) {
            project.setActualCompletionDate(LocalDate.now());
        }

        // Record history
        recordStatusChange(project, currentStatus, newStatus,
                reworkNumber, request.getRemarks(), currentUsername());

        // Update status
        project.setStatus(newStatus);

        Project saved = projectRepository.save(project);
        log.info(" Status updated: {} → {}", currentStatus, newStatus);
        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectStatusHistoryResponse> getProjectStatusHistory(UUID id) {
        findActiveProject(id);
        List<ProjectStatusHistory> history =
                historyRepository.findByProjectIdOrderByChangedAtDesc(id);
        return projectMapper.toHistoryResponseList(history);
    }

    // ============================================================
    // USER LOOKUPS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AssignableUserResponse> getAllProjectManagers() {
        List<User> managers = userRepository.findAllByRoleName(RoleName.PROJECT_MANAGER);
        log.debug("Found {} project managers", managers.size());
        return projectMapper.toAssignableUserResponseList(managers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignableUserResponse> getAllEmployees() {
        List<User> employees = userRepository.findAllByRoleName(RoleName.EMPLOYEE);
        log.debug("Found {} employees", employees.size());
        return projectMapper.toAssignableUserResponseList(employees);
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private Project findActiveProject(UUID id) {
        return projectRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found or has been deleted: " + id));
    }

    /**
     * Apply assignment rules when creating
     */
    private void applyCreateAssignments(Project project, ProjectCreateRequest request) {
        if (request.getProjectType() == ProjectType.BIG) {
            // BIG: Manager REQUIRED, Employee OPTIONAL
            if (request.getProjectLeadId() == null) {
                throw new BusinessException(
                        "Project lead (PROJECT_MANAGER) is required for BIG projects");
            }

            User projectLead = resolveUserByRole(
                    request.getProjectLeadId(), RoleName.PROJECT_MANAGER, "project lead");
            project.setProjectLead(projectLead);

            if (request.getAssignedEmployeeId() != null) {
                User employee = resolveUserByRole(
                        request.getAssignedEmployeeId(), RoleName.EMPLOYEE, "assigned employee");
                project.setAssignedEmployee(employee);
            } else {
                project.setAssignedEmployee(null);
            }

        } else { // SMALL
            // : At least ONE of Manager OR Employee REQUIRED
            if (request.getProjectLeadId() == null && request.getAssignedEmployeeId() == null) {
                throw new BusinessException(
                        "For SMALL projects, either a Project Manager or an Employee must be assigned");
            }

            if (request.getProjectLeadId() != null) {
                User projectLead = resolveUserByRole(
                        request.getProjectLeadId(), RoleName.PROJECT_MANAGER, "project lead");
                project.setProjectLead(projectLead);
            } else {
                project.setProjectLead(null);
            }

            if (request.getAssignedEmployeeId() != null) {
                User employee = resolveUserByRole(
                        request.getAssignedEmployeeId(), RoleName.EMPLOYEE, "assigned employee");
                project.setAssignedEmployee(employee);
            } else {
                project.setAssignedEmployee(null);
            }
        }
    }

    /**
     * Apply assignment rules when updating
     */
    private void applyUpdateAssignments(Project project, ProjectUpdateRequest request) {
        ProjectType effectiveType = project.getProjectType();

        if (effectiveType == ProjectType.BIG) {
            UUID effectiveLeadId = request.getProjectLeadId() != null
                    ? request.getProjectLeadId()
                    : (project.getProjectLead() != null ? project.getProjectLead().getId() : null);

            if (effectiveLeadId == null) {
                throw new BusinessException("Project lead is required for BIG projects");
            }

            User projectLead = resolveUserByRole(
                    effectiveLeadId, RoleName.PROJECT_MANAGER, "project lead");
            project.setProjectLead(projectLead);

            if (request.getAssignedEmployeeId() != null) {
                User employee = resolveUserByRole(
                        request.getAssignedEmployeeId(), RoleName.EMPLOYEE, "assigned employee");
                project.setAssignedEmployee(employee);
            }

        } else { // SMALL
            UUID effectiveLeadId = request.getProjectLeadId() != null
                    ? request.getProjectLeadId()
                    : (project.getProjectLead() != null ? project.getProjectLead().getId() : null);

            UUID effectiveEmployeeId = request.getAssignedEmployeeId() != null
                    ? request.getAssignedEmployeeId()
                    : (project.getAssignedEmployee() != null
                       ? project.getAssignedEmployee().getId() : null);

            if (effectiveLeadId == null && effectiveEmployeeId == null) {
                throw new BusinessException(
                        "For SMALL projects, either a Project Manager or an Employee must be assigned");
            }

            if (effectiveLeadId != null) {
                User projectLead = resolveUserByRole(
                        effectiveLeadId, RoleName.PROJECT_MANAGER, "project lead");
                project.setProjectLead(projectLead);
            } else {
                project.setProjectLead(null);
            }

            if (effectiveEmployeeId != null) {
                User employee = resolveUserByRole(
                        effectiveEmployeeId, RoleName.EMPLOYEE, "assigned employee");
                project.setAssignedEmployee(employee);
            } else {
                project.setAssignedEmployee(null);
            }
        }
    }

    private User resolveUserByRole(UUID userId, RoleName requiredRole, String fieldName) {
        User user = userRepository.findByIdAndDeletedFalseAndEnabledTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found or inactive with id: " + userId));

        boolean hasRole = user.getRoles().stream()
                .anyMatch(role -> requiredRole.equals(role.getName()));

        if (!hasRole) {
            throw new BusinessException(
                    String.format("Selected user for %s does not have role %s",
                            fieldName, requiredRole.name()));
        }

        return user;
    }

    private void recordStatusChange(Project project,
                                    ProjectStatus previousStatus,
                                    ProjectStatus newStatus,
                                    Integer reworkNumber,
                                    String remarks,
                                    String changedBy) {
        ProjectStatusHistory history = ProjectStatusHistory.builder()
                .project(project)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reworkNumber(reworkNumber)
                .remarks(remarks)
                .changedBy(changedBy)
                .build();

        historyRepository.save(history);
    }

    private void validateDateRange(LocalDate startDate, LocalDate expectedEnd) {
        if (startDate != null && expectedEnd != null && expectedEnd.isBefore(startDate)) {
            throw new BusinessException(
                    "Expected completion date cannot be before the start date");
        }
    }

    private String currentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}