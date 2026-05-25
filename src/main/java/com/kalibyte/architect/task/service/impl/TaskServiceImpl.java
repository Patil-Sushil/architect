package com.kalibyte.architect.task.service.impl;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.repository.UserRepository;
import com.kalibyte.architect.common.exception.BusinessException;
import com.kalibyte.architect.common.exception.ResourceNotFoundException;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.common.util.SecurityUtils;
import com.kalibyte.architect.project.entity.Project;
import com.kalibyte.architect.project.repository.ProjectRepository;
import com.kalibyte.architect.storage.service.FileStorageService;
import com.kalibyte.architect.task.dto.request.*;
import com.kalibyte.architect.task.dto.response.*;
import com.kalibyte.architect.task.entity.*;
import com.kalibyte.architect.task.entity.enums.*;
import com.kalibyte.architect.task.mapper.TaskMapper;
import com.kalibyte.architect.task.repository.*;
import com.kalibyte.architect.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskTemplateRepository templateRepository;
    private final TaskHistoryRepository historyRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;
    private final FileStorageService fileStorageService;


    @Override
    @Transactional(readOnly = true)
    public List<TaskTemplateResponse> getAllSuggestions() {
        return templateRepository.findAll().stream()
                .map(taskMapper::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskTemplateResponse createNewSuggestion(String name) {
        String trimmedName = name.trim();
        return templateRepository.findByNameIgnoreCase(trimmedName)
                .map(taskMapper::toTemplateResponse)
                .orElseGet(() -> {
                    TaskTemplate newTemplate = TaskTemplate.builder()
                            .name(trimmedName)
                            .isCustomAdded(true)
                            .build();
                    return taskMapper.toTemplateResponse(templateRepository.save(newTemplate));
                });
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        log.info("Creating task: {}", request.getTaskName());
        
        User currentUser = getCurrentUser();
        User assignedTo = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));

        // Resolve Task Name and Template
        TaskTemplate template = null;
        String finalTaskName = request.getTaskName().trim();
        
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
            finalTaskName = template.getName();
        } else {
            // Check if handwritten name matches existing template
            var existingTemplate = templateRepository.findByNameIgnoreCase(finalTaskName);
            if (existingTemplate.isPresent()) {
                template = existingTemplate.get();
                finalTaskName = template.getName();
            } else {
                // Auto-save as new suggestion
                TaskTemplate newTemplate = TaskTemplate.builder()
                        .name(finalTaskName)
                        .isCustomAdded(true)
                        .build();
                template = templateRepository.save(newTemplate);
            }
        }

        // Resolve Source
        TaskSource source = request.getSource();
        if (source == null) {
            if (request.getProjectId() != null) {
                source = TaskSource.PROJECT_DRIVEN;
            } else if (request.getPriority() == TaskPriority.HIGH || request.getPriority() == TaskPriority.URGENT) {
                source = TaskSource.FIELD_DIRECT;
            } else {
                source = TaskSource.GENERAL_INTERNAL;
            }
        }

        // Resolve Job Number
        String jobNumber = request.getJobNumber();
        if (jobNumber == null || jobNumber.isBlank()) {
            jobNumber = generateJobNumber();
        }

        // Resolve Project
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
        }

        Task task = Task.builder()
                .jobNumber(jobNumber)
                .taskName(finalTaskName)
                .template(template)
                .category(request.getCategory())
                .description(request.getDescription())
                .project(project)
                .assignedTo(assignedTo)
                .taskCreator(currentUser)
                .source(source)
                .priority(request.getPriority())
                .status(TaskStatus.ASSIGNED)
                .referenceType(request.getReferenceType())
                .referredBy(request.getReferredBy())
                .plannedStartDate(request.getPlannedStartDate())
                .plannedEndDate(request.getPlannedEndDate())
                .plannedEffortsHours(request.getPlannedEffortsHours())
                .build();

        task = taskRepository.save(task);
        
        // Initial history log
        logHistory(task, null, TaskStatus.ASSIGNED, currentUser, "Task created and assigned");
        
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse submitTaskForReview(UUID taskId, TaskSubmitRequest request, List<MultipartFile> files) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (task.getStatus() != TaskStatus.ASSIGNED && task.getStatus() != TaskStatus.REWORK_REQUESTED) {
            throw new BusinessException("Task can only be submitted if it's in ASSIGNED or REWORK_REQUESTED state");
        }

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(TaskStatus.UNDER_REVIEW);
        task.setActualEffortsHours(request.getHoursInvested());

        // --- Process uploaded files ---
        int attachmentCount = 0;
        if (!CollectionUtils.isEmpty(files)) {
            List<TaskAttachment> newAttachments = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    log.warn("Skipping blank file entry in submission for task {}", taskId);
                    continue;
                }

                // Delegate to the storage abstraction — no local I/O here
                String storedPath = fileStorageService.storeFile(file, "tasks");

                // Derive the file type from the original extension (lowercase, no dot)
                String originalName = file.getOriginalFilename() != null
                        ? file.getOriginalFilename()
                        : "unknown";
                String fileType = deriveFileType(originalName);

                TaskAttachment attachment = TaskAttachment.builder()
                        .task(task)
                        .fileName(originalName)
                        .filePath(storedPath)
                        .fileType(fileType)
                        .build();

                newAttachments.add(attachment);
                attachmentCount++;

                log.debug("Queued attachment '{}' -> '{}' for task {}",
                        originalName, storedPath, taskId);
            }
            task.getAttachments().addAll(newAttachments);
        }

        // --- Audit history: include file count for traceability ---
        String historyNote = buildSubmissionNote(request.getSubmissionNotes(), attachmentCount);
        User employee = getCurrentUser();
        logHistory(task, oldStatus, TaskStatus.UNDER_REVIEW, employee, historyNote);

        log.info("Task {} submitted for review by user {} with {} attachment(s)",
                taskId, employee.getId(), attachmentCount);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse reviewTask(UUID taskId, TaskReviewRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (task.getStatus() != TaskStatus.UNDER_REVIEW) {
            throw new BusinessException("Only tasks UNDER_REVIEW can be reviewed");
        }

        TaskStatus oldStatus = task.getStatus();
        TaskStatus nextStatus = request.getApproved() ? TaskStatus.COMPLETED : TaskStatus.REWORK_REQUESTED;
        
        task.setStatus(nextStatus);
        if (nextStatus == TaskStatus.COMPLETED) {
            // Logic to calculate actual effort could be added here if tracked
        }

        User reviewer = getCurrentUser();
        logHistory(task, oldStatus, nextStatus, reviewer, request.getReviewComment());
        
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskHistoryResponse> getTaskTimeline(UUID taskId) {
        List<TaskHistory> history = historyRepository.findByTaskIdOrderByTimestampAsc(taskId);
        return taskMapper.toHistoryResponseList(history);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getMyTasks(TaskStatus status, TaskPriority priority, UUID projectId, int page, int size) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        var taskPage = taskRepository.findMyTasks(currentUserId, status, priority, projectId, pageable);
        
        return taskMapper.toPageResponse(taskPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getAllTasks(TaskStatus status, TaskPriority priority, UUID projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        var taskPage = taskRepository.findAllTasks(status, priority, projectId, pageable);
        
        return taskMapper.toPageResponse(taskPage);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        return taskRepository.findById(taskId)
                .map(taskMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskHistoryResponse> getGlobalTaskHistory(UUID taskId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<TaskHistory> historyPage;
        if (taskId != null) {
            historyPage = historyRepository.findByTaskId(taskId, pageable);
        } else {
            historyPage = historyRepository.findAll(pageable);
        }
        return taskMapper.toHistoryPageResponse(historyPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskHistoryResponse> getMyTaskHistory(int page, int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<TaskHistory> historyPage = historyRepository.findMyHistory(userId, pageable);
        return taskMapper.toHistoryPageResponse(historyPage);
    }

    private void logHistory(Task task, TaskStatus from, TaskStatus to, User user, String remarks) {
        TaskHistory history = TaskHistory.builder()
                .task(task)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(user)
                .remarks(remarks)
                .build();
        historyRepository.save(history);
    }

    private User getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private String generateJobNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String hash = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TASK-GEN-%s-%s", year, hash);
    }

    /**
     * Extracts the lowercase file extension from a filename (without the leading dot).
     * Returns an empty string if no extension is present.
     * Example: "ground_floor.DWG" → "dwg"
     */
    private String deriveFileType(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Builds the history-log note, appending the attachment count so reviewers
     * know immediately how many files were submitted alongside the notes.
     */
    private String buildSubmissionNote(String submissionNotes, int fileCount) {
        if (fileCount == 0) {
            return submissionNotes;
        }
        return String.format("%s [%d file(s) attached]", submissionNotes, fileCount);
    }

    // -------------------------------------------------------------------------
    // Attachment read methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getTaskAttachments(UUID taskId) {
        // Verify the task itself exists first so we return 404 for unknown tasks
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        List<TaskAttachment> attachments =
                attachmentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        return taskMapper.toAttachmentResponseList(attachments);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload getAttachmentForDownload(UUID taskId, UUID attachmentId) {
        TaskAttachment attachment = attachmentRepository
                .findByIdAndTaskId(attachmentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attachment not found with id " + attachmentId + " for task " + taskId));

        log.info("Serving attachment '{}' (task: {}, attachment: {})",
                attachment.getFileName(), taskId, attachmentId);

        return new FileDownload(
                fileStorageService.loadFile(attachment.getFilePath()),
                attachment.getFileName());
    }
}
