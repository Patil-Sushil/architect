package com.kalibyte.architect.attendance.repository;

import com.kalibyte.architect.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByUserIdAndDate(UUID userId, LocalDate date);

    Optional<Attendance> findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(UUID userId);

    List<Attendance> findAllByLogoutTimeIsNullAndLoginTimeBefore(java.time.LocalDateTime time);

    @Query("SELECT a FROM Attendance a WHERE a.logoutTime IS NULL AND a.date = :date")
    List<Attendance> findAllByLogoutTimeIsNullAndDate(@Param("date") LocalDate date);
}
