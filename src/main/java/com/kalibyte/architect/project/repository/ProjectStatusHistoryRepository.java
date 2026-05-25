package com.kalibyte.architect.project.repository;

import com.kalibyte.architect.project.entity.ProjectStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectStatusHistoryRepository
        extends JpaRepository<ProjectStatusHistory, UUID> {

    List<ProjectStatusHistory> findByProjectIdOrderByChangedAtDesc(UUID projectId);
}