package com.kalibyte.architect.attendance.service;

import com.kalibyte.architect.attendance.dto.AttendanceExceptionRequest;
import com.kalibyte.architect.attendance.dto.AttendanceResponse;
import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.common.util.DateRangePreset;

import java.time.LocalDate;
import java.util.UUID;

public interface AttendanceService {

    void automaticLogin(User user);

    void automaticLogout(UUID userId);

    void processNightlyClosure();

    AttendanceResponse handleManualAttendance(AttendanceExceptionRequest request);

    PageResponse<AttendanceResponse> getFilteredRecords(UUID userId,DateRangePreset preset, LocalDate startDate, LocalDate endDate, int page, int size);

    PageResponse<AttendanceResponse> getMyRecords(DateRangePreset preset, LocalDate startDate, LocalDate endDate, int page, int size);

    byte[] exportAttendanceToExcel(DateRangePreset preset, LocalDate startDate, LocalDate endDate, UUID userId);
}
