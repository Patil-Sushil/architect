package com.kalibyte.architect.task.repository;

import com.kalibyte.architect.task.entity.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, UUID> {
    Optional<TaskTemplate> findByNameIgnoreCase(String name);
}
