package com.kalibyte.architect.audit.controller;

import com.kalibyte.architect.audit.dto.request.AuditLogFilterRequest;
import com.kalibyte.architect.audit.dto.response.AuditLogResponse;
import com.kalibyte.architect.audit.dto.response.AuditStatisticsResponse;
import com.kalibyte.architect.audit.dto.response.UserActivityResponse;
import com.kalibyte.architect.audit.entity.enums.AuditAction;
import com.kalibyte.architect.audit.service.AuditLogService;
import com.kalibyte.architect.common.annotation.LoggableAction;
import com.kalibyte.architect.common.response.ApiResponse;
import com.kalibyte.architect.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Get all audit logs with advanced filtering
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve paginated and filtered audit logs",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAllLogs(
            @ModelAttribute AuditLogFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getAllLogs(filter, pageable))
        );
    }

    /**
     * Get audit log by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve a single audit log by ID",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<AuditLogResponse>> getLogById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getLogById(id)));
    }

    /**
     * Get user activity logs
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve activity logs for a specific user",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getUserActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getUserActivity(userId, pageable))
        );
    }

    /**
     * Get entity-specific audit trail
     */
    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @LoggableAction(
            value = "Retrieve audit trail for a specific entity",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {

        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getEntityAuditTrail(entityType, entityId))
        );
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve audit log statistics",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<AuditStatisticsResponse>> getStatistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getStatistics(startDate, endDate))
        );
    }

    /**
     * Get top active users
     */
    @GetMapping("/top-users")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve top active users",
            action = AuditAction.GET_TOP_ACTIVE_USERS,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<List<UserActivityResponse>>> getTopActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getTopActiveUsers(limit))
        );
    }

    /**
     * Get recent logs
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Retrieve recent audit logs",
            action = AuditAction.OTHER,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "24") int hours) {

        return ResponseEntity.ok(
                ApiResponse.success(auditLogService.getRecentLogs(hours))
        );
    }

    @PostMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    @LoggableAction(
            value = "Export audit logs to CSV",
            action = AuditAction.EXPORT_AUDIT_LOGS,
            entityType = "AUDIT_LOG"
    )
    public ResponseEntity<byte[]> exportLogsToCSV(@RequestBody AuditLogFilterRequest filter) {
        byte[] csvData = auditLogService.exportLogsToCSV(filter);

        String filename = String.format("audit_logs_%s.csv",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}