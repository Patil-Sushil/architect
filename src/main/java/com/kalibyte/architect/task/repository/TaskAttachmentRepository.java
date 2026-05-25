package com.kalibyte.architect.task.repository;

import com.kalibyte.architect.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

    /**
     * Fetches all attachments for a given task, eager-loading the creator
     * to avoid N+1 when rendering the attachment list in the task detail view.
     */
    @Query("SELECT a FROM TaskAttachment a " +
           "LEFT JOIN FETCH a.task t " +
           "WHERE t.id = :taskId " +
           "ORDER BY a.createdAt ASC")
    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") UUID taskId);

    /**
     * Looks up a single attachment, ensuring it actually belongs to the stated task.
     * Used in the download endpoint to prevent arbitrary-ID lookups across tasks.
     */
    @Query("SELECT a FROM TaskAttachment a WHERE a.id = :attachmentId AND a.task.id = :taskId")
    Optional<TaskAttachment> findByIdAndTaskId(
            @Param("attachmentId") UUID attachmentId,
            @Param("taskId") UUID taskId);
}
