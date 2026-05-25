package com.kalibyte.architect.auth.repository;

import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.entity.enums.RoleName;
import com.kalibyte.architect.auth.entity.enums.SkillType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            WHERE r.name = :roleName
            AND u.deleted = false
            AND u.enabled = true
            ORDER BY u.name ASC
            """)
    List<User> findAllByRoleName(@Param("roleName") RoleName roleName);

    java.util.List<User> findBySkillsContaining(SkillType skill);
    Page<User> findBySkillsContaining(SkillType skill, Pageable pageable);

    Optional<User> findByIdAndDeletedFalseAndEnabledTrue(UUID id);
}
