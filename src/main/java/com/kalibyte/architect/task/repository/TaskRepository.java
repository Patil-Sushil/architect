package com.kalibyte.architect.task.repository;

import com.kalibyte.architect.task.entity.Task;
import com.kalibyte.architect.task.entity.enums.TaskPriority;
import com.kalibyte.architect.task.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:projectId IS NULL OR t.project.id = :projectId)")
    Page<Task> findMyTasks(
            @Param("userId") UUID userId,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:projectId IS NULL OR t.project.id = :projectId)")
    Page<Task> findAllTasks(
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );
}
