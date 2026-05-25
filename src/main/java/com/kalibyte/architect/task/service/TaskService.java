package com.kalibyte.architect.task.service;

import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.task.dto.request.*;
import com.kalibyte.architect.task.dto.response.*;
import com.kalibyte.architect.task.entity.enums.TaskPriority;
import com.kalibyte.architect.task.entity.enums.TaskStatus;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TaskService {

    /**
     * Typed carrier returned by {@link #getAttachmentForDownload} so the controller
     * can set the correct filename in the {@code Content-Disposition} header without
     * knowing anything about the storage implementation.
     */
    record FileDownload(Resource resource, String fileName) {}

    List<TaskTemplateResponse> getAllSuggestions();
    TaskTemplateResponse createNewSuggestion(String name);
    TaskResponse createTask(TaskCreateRequest request);

    /**
     * Transitions a task to UNDER_REVIEW status, persisting any uploaded files
     * as {@link com.kalibyte.architect.task.entity.TaskAttachment} records.
     *
     * @param taskId  the task to submit
     * @param request JSON metadata (notes, hours invested)
     * @param files   optional list of binary attachments; may be {@code null} or empty
     */
    TaskResponse submitTaskForReview(UUID taskId, TaskSubmitRequest request, List<MultipartFile> files);

    TaskResponse reviewTask(UUID taskId, TaskReviewRequest request);
    List<TaskHistoryResponse> getTaskTimeline(UUID taskId);
    PageResponse<TaskResponse> getMyTasks(TaskStatus status, TaskPriority priority, UUID projectId, int page, int size);
    PageResponse<TaskResponse> getAllTasks(TaskStatus status, TaskPriority priority, UUID projectId, int page, int size);
    TaskResponse getTaskById(UUID taskId);
    PageResponse<TaskHistoryResponse> getGlobalTaskHistory(UUID taskId, int page, int size);
    PageResponse<TaskHistoryResponse> getMyTaskHistory(int page, int size);

    /**
     * Returns the metadata list of all files attached to a task.
     * Used by the list-attachments endpoint.
     */
    List<TaskAttachmentResponse> getTaskAttachments(UUID taskId);

    /**
     * Resolves an attachment record (verifying it belongs to {@code taskId}) and
     * loads the backing file from storage, returning both the {@link Resource}
     * and the original filename for use in the HTTP response.
     *
     * @throws com.kalibyte.architect.common.exception.ResourceNotFoundException
     *         if the attachment doesn't exist or doesn't belong to the task
     */
    FileDownload getAttachmentForDownload(UUID taskId, UUID attachmentId);
}
