package com.kalibyte.architect.task.repository;

import com.kalibyte.architect.task.entity.TaskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
    List<TaskHistory> findByTaskIdOrderByTimestampAsc(UUID taskId);

    @Query("SELECT th FROM TaskHistory th WHERE th.task.id = :taskId")
    Page<TaskHistory> findByTaskId(@Param("taskId") UUID taskId, Pageable pageable);

    @Query("SELECT th FROM TaskHistory th JOIN th.task t WHERE " +
           "(t.assignedTo.id = :userId OR th.changedBy.id = :userId)")
    Page<TaskHistory> findMyHistory(@Param("userId") UUID userId, Pageable pageable);
}
