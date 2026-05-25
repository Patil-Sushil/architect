package com.kalibyte.architect.task.controller;

import com.kalibyte.architect.common.annotation.LoggableAction;
import com.kalibyte.architect.common.response.ApiResponse;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.task.dto.request.*;
import com.kalibyte.architect.task.dto.response.*;
import com.kalibyte.architect.task.entity.enums.TaskPriority;
import com.kalibyte.architect.task.entity.enums.TaskStatus;
import com.kalibyte.architect.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getSuggestions() {
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllSuggestions()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER', 'EMPLOYEE')")
    @LoggableAction("Create a new task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", taskService.createTask(request)));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getMyTasks(status, priority, projectId, page, size)));
    }

    @GetMapping("/allTasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction("FETCHED_ALL_TASKS")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getAllTasks(status, priority, projectId, page, size)));
    }

    /**
     * Submits a task for manager review, optionally attaching CAD drawings, Revit files, PDFs, etc.
     *
     * <p>The request must be sent as {@code multipart/form-data}:
     * <ul>
     *   <li><b>submission</b> – JSON blob matching {@link TaskSubmitRequest} (Content-Type: application/json)</li>
     *   <li><b>files</b> – one or more binary file parts (optional)</li>
     * </ul>
     */
    @PostMapping(value = "/{taskId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @LoggableAction("Submit task for review")
    public ResponseEntity<ApiResponse<TaskResponse>> submitTask(
            @PathVariable UUID taskId,
            @RequestPart("submission") @Valid TaskSubmitRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                "Task submitted for review",
                taskService.submitTaskForReview(taskId, request, files)));
    }

    @PostMapping("/{taskId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction("Review submitted task")
    public ResponseEntity<ApiResponse<TaskResponse>> reviewTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskReviewRequest request) {
        String message = request.getApproved() ? "Task approved and completed" : "Rework requested for task";
        return ResponseEntity.ok(ApiResponse.success(message, taskService.reviewTask(taskId, request)));
    }

    @GetMapping("/{taskId}/timeline")
    @LoggableAction("FETCHED_TIMELINE_FOR_TASK")
    public ResponseEntity<ApiResponse<List<TaskHistoryResponse>>> getTimeline(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskTimeline(taskId)));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(taskId)));
    }

    // -------------------------------------------------------------------------
    // Attachment endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns the list of file attachments for a task.
     * Admins and PMs use this to browse submitted files before or after reviewing.
     */
    @GetMapping("/{taskId}/attachments")
    @LoggableAction("List task attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> listAttachments(
            @PathVariable UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskAttachments(taskId)));
    }

    /**
     * Streams the raw file back to the client with the correct MIME type and
     * {@code Content-Disposition: attachment} header so browsers trigger a download.
     *
     * <p>Access is intentionally open to all authenticated users so the employee
     * who submitted the task can also re-download their own files.
     */
    @GetMapping("/{taskId}/attachments/{attachmentId}/download")
    @LoggableAction("Download task attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId) {

        TaskService.FileDownload download = taskService.getAttachmentForDownload(taskId, attachmentId);

        // Detect MIME type from the stored original filename; fall back to octet-stream
        MediaType mediaType = MediaTypeFactory
                .getMediaType(download.fileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(download.fileName())
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(download.resource());
    }
}
