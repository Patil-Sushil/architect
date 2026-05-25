package com.kalibyte.architect.attendance.controller;

import com.kalibyte.architect.attendance.dto.AttendanceExceptionRequest;
import com.kalibyte.architect.attendance.dto.AttendanceResponse;
import com.kalibyte.architect.attendance.service.AttendanceService;
import com.kalibyte.architect.common.response.ApiResponse;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.common.util.DateRangePreset;
import com.kalibyte.architect.common.util.DateRangeRequest;
import com.kalibyte.architect.common.util.DateRangeResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/admin/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> getAdminRecords(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) DateRangePreset preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var response = attendanceService.getFilteredRecords(userId, preset, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Attendance records retrieved successfully", response));
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam(required = false) DateRangePreset preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID userId) {

        byte[] excelContent = attendanceService.exportAttendanceToExcel(preset, startDate, endDate, userId);

        // Resolve dates again for filename accuracy
        DateRangeRequest request = DateRangeRequest.builder()
                .preset(preset)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        DateRangeResolver.DateRange range = DateRangeResolver.resolve(request);

        String filename = String.format("attendance_report_%s_to_%s.xlsx",
                range.startDate(), range.endDate());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @PostMapping("/admin/exception")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> handleException(
            @Valid @RequestBody AttendanceExceptionRequest request) {

        var response = attendanceService.handleManualAttendance(request);
        return ResponseEntity.ok(ApiResponse.success("Attendance record updated successfully", response));
    }

    @GetMapping("/my-records")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> getMyRecords(
            @RequestParam(required = false) DateRangePreset preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var response = attendanceService.getMyRecords(preset, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Your attendance records retrieved successfully", response));
    }
}
