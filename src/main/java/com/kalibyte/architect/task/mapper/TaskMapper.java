package com.kalibyte.architect.task.mapper;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.task.dto.response.*;
import com.kalibyte.architect.task.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class TaskMapper {

    @Mapping(target = "projectId",        source = "project.id")
    @Mapping(target = "projectName",      source = "project.projectName")
    @Mapping(target = "projectJobNumber", source = "project.jobNumber")
    @Mapping(target = "assignedTo",       source = "assignedTo")
    @Mapping(target = "createdBy",        source = "taskCreator")
    @Mapping(target = "attachments",      source = "attachments")
    public abstract TaskResponse toResponse(Task task);

    public abstract TaskTemplateResponse toTemplateResponse(TaskTemplate template);

    @Mapping(target = "changedBy", source = "changedBy")
    public abstract TaskHistoryResponse toHistoryResponse(TaskHistory history);

    public abstract List<TaskHistoryResponse> toHistoryResponseList(List<TaskHistory> historyList);

    public abstract TaskResponse.UserSummary toUserSummary(User user);

    /**
     * Maps a {@link TaskAttachment} entity to its response DTO.
     *
     * <p>The {@code downloadUrl} is constructed here rather than stored in the DB,
     * keeping the API path as the single source of truth for navigation.
     * The {@code uploadedAt} and {@code uploadedBy} are read from {@code BaseEntity}
     * audit fields ({@code createdAt} / {@code createdBy}).
     */
    @Mapping(target = "uploadedAt",   source = "createdAt")
    @Mapping(target = "uploadedBy",   source = "createdBy")
    @Mapping(target = "downloadUrl",  expression = "java(buildDownloadUrl(attachment.getTask().getId(), attachment.getId()))")
    public abstract TaskAttachmentResponse toAttachmentResponse(TaskAttachment attachment);

    public abstract List<TaskAttachmentResponse> toAttachmentResponseList(List<TaskAttachment> attachments);

    /**
     * Constructs the relative API URL a client should call to download the file.
     * Using a method makes it easy to change the URL pattern in one place.
     */
    protected String buildDownloadUrl(UUID taskId, UUID attachmentId) {
        return String.format("/api/tasks/%s/attachments/%s/download", taskId, attachmentId);
    }

    public PageResponse<TaskResponse> toPageResponse(Page<Task> taskPage) {
        return PageResponse.from(taskPage, this::toResponse);
    }

    public PageResponse<TaskHistoryResponse> toHistoryPageResponse(Page<TaskHistory> historyPage) {
        return PageResponse.from(historyPage, this::toHistoryResponse);
    }
}
