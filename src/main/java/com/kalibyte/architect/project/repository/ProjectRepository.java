package com.kalibyte.architect.project.repository;

import com.kalibyte.architect.project.entity.Project;
import com.kalibyte.architect.project.entity.enums.ProjectStatus;
import com.kalibyte.architect.project.entity.enums.ProjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByJobNumber(String jobNumber);

    boolean existsByJobNumberAndIdNot(String jobNumber, UUID id);

    Optional<Project> findByIdAndDeletedFalse(UUID id);

    /**
     * Fetch project with users eagerly loaded
     */
    @Query("""
            SELECT p FROM Project p
            LEFT JOIN FETCH p.projectLead
            LEFT JOIN FETCH p.assignedEmployee
            WHERE p.id = :id
            AND p.deleted = false
            """)
    Optional<Project> findByIdWithUsers(@Param("id") UUID id);

    /**
     * Advanced filtering query with proper UUID handling
     *  Cast UUID to VARCHAR before applying LOWER()
     */
    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN p.projectLead pl
            LEFT JOIN p.assignedEmployee ae
            WHERE p.deleted = false
            AND (:status IS NULL OR p.status = :status)
            AND (:projectType IS NULL OR p.projectType = :projectType)
            AND (:projectLeadId IS NULL OR pl.id = :projectLeadId)
            AND (:assignedEmployeeId IS NULL OR ae.id = :assignedEmployeeId)
            AND (
                :search IS NULL OR :search = ''
                OR LOWER(p.jobNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.clientOwnerName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.siteLocation) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<Project> findAllWithFilters(
            @Param("status")             ProjectStatus status,
            @Param("projectType")        ProjectType projectType,
            @Param("projectLeadId")      UUID projectLeadId,
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("search")             String search,
            Pageable pageable
    );

    /**
     * Count projects by status
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status AND p.deleted = false")
    long countByStatus(@Param("status") ProjectStatus status);

    /**
     * Count projects by type
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.projectType = :type AND p.deleted = false")
    long countByProjectType(@Param("type") ProjectType type);
}