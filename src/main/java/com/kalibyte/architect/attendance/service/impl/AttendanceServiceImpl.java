package com.kalibyte.architect.attendance.service.impl;

import com.kalibyte.architect.attendance.dto.AttendanceExceptionRequest;
import com.kalibyte.architect.attendance.dto.AttendanceResponse;
import com.kalibyte.architect.attendance.entity.Attendance;
import com.kalibyte.architect.attendance.mapper.AttendanceMapper;
import com.kalibyte.architect.attendance.repository.AttendanceRepository;
import com.kalibyte.architect.attendance.repository.specification.AttendanceSpecification;
import com.kalibyte.architect.attendance.service.AttendanceService;
import com.kalibyte.architect.auth.entity.User;
import com.kalibyte.architect.auth.entity.enums.RoleName;
import com.kalibyte.architect.auth.repository.UserRepository;
import com.kalibyte.architect.common.annotation.LoggableAction;
import com.kalibyte.architect.common.exception.AttendanceAlreadyMarkedException;
import com.kalibyte.architect.common.exception.BusinessException;
import com.kalibyte.architect.common.exception.ResourceNotFoundException;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.common.util.SecurityUtils;
import com.kalibyte.architect.common.util.DateRangePreset;
import com.kalibyte.architect.common.util.DateRangeRequest;
import com.kalibyte.architect.common.util.DateRangeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    @Transactional(noRollbackFor = AttendanceAlreadyMarkedException.class)
    public void automaticLogin(User user) {
        // Skip attendance tracking for ADMIN users
        if (isAdmin(user)) {
            log.info("Skipping attendance generation for ADMIN user: {}", user.getEmail());
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Check for open record from yesterday (Night Shift support)
        Optional<Attendance> openYesterday = attendanceRepository.findByUserIdAndDate(user.getId(), yesterday)
                .filter(a -> a.getLogoutTime() == null);

        if (openYesterday.isPresent()) {
            log.info("User {} has an active night shift from yesterday. Skipping new login.", user.getEmail());
            return;
        }

        // Check if already logged in today
        Optional<Attendance> todayRecord = attendanceRepository.findByUserIdAndDate(user.getId(), today);
        if (todayRecord.isPresent()) {
            // Reopen existing record for today (Multi-Punch support)
            Attendance attendance = todayRecord.get();
            attendance.setLogoutTime(null);
            attendance.setTotalHours(null);
            attendanceRepository.save(attendance);
            log.info("Reopened attendance record for user: {} today", user.getEmail());
            return;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .date(today)
                .loginTime(LocalDateTime.now())
                .build();
        attendanceRepository.save(attendance);
        log.info("Automatic login for user: {} at {}", user.getEmail(), attendance.getLoginTime());
    }

    @Override
    @Transactional
    public void automaticLogout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Skip attendance tracking for ADMIN users
        if (isAdmin(user)) {
            log.info("Skipping attendance tracking for ADMIN user: {}", user.getEmail());
            return;
        }

        // Find most recent open record regardless of whether it's today or yesterday
        Attendance attendance = attendanceRepository.findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId)
                .orElseThrow(() -> new BusinessException("No open attendance record found for user"));

        attendance.setLogoutTime(LocalDateTime.now());
        attendance.setTotalHours(calculateHours(attendance.getLoginTime(), attendance.getLogoutTime()));
        attendanceRepository.save(attendance);
        log.info("Automatic logout for user: {} at {}", userId, attendance.getLogoutTime());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    @Override
    @Scheduled(cron = "0 0 4 * * *") // Run daily at 4:00 AM
    @Transactional
    public void processNightlyClosure() {
        log.info("Starting nightly attendance closure process");
        
        // A record is ONLY auto-closed if it has been open for > 16 hours (unrealistic shift)
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(16);
        List<Attendance> openRecords = attendanceRepository.findAllByLogoutTimeIsNullAndLoginTimeBefore(threshold);

        for (Attendance attendance : openRecords) {
            // Set logoutTime to exactly 9 hours after login (standard shift)
            LocalDateTime closureTime = attendance.getLoginTime().plusHours(9);
            attendance.setLogoutTime(closureTime);
            attendance.setTotalHours(9.0);
            attendance.setExceptionNote("System closed: Missing checkout after 16-hour threshold");
            attendanceRepository.save(attendance);
        }
        log.info("Nightly closure completed. Processed {} records", openRecords.size());
    }

    @Override
    @Transactional
    @LoggableAction("MANUAL_ATTENDANCE_UPDATE")
    public AttendanceResponse handleManualAttendance(AttendanceExceptionRequest request) {
        // Allows HR/ADMIN to manually create or update an attendance record (e.g., for accidental logouts)
        if (request.getLogoutTime() != null && request.getLogoutTime().isBefore(request.getLoginTime())) {
            throw new BusinessException("Logout time cannot be before login time");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Attendance attendance = attendanceRepository.findByUserIdAndDate(request.getUserId(), request.getDate())
                .orElse(Attendance.builder()
                        .user(user)
                        .date(request.getDate())
                        .build());

        attendance.setLoginTime(request.getLoginTime());
        attendance.setLogoutTime(request.getLogoutTime());
        attendance.setExceptionNote(request.getExceptionNote());
        attendance.setIsManuallyEdited(true);

        if (attendance.getLogoutTime() != null) {
            attendance.setTotalHours(calculateHours(attendance.getLoginTime(), attendance.getLogoutTime()));
        }

        return attendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getFilteredRecords(UUID userId, DateRangePreset preset, LocalDate startDate, LocalDate endDate, int page, int size) {
        DateRangeRequest dateRangeRequest = DateRangeRequest.builder()
                .preset(preset)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        DateRangeResolver.DateRange range = DateRangeResolver.resolve(dateRangeRequest);

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        var spec = AttendanceSpecification.filterRecords(userId, range.startDate(), range.endDate());
        var attendancePage = attendanceRepository.findAll(spec, pageable);
        return PageResponse.from(attendancePage, attendanceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getMyRecords(DateRangePreset preset, LocalDate startDate, LocalDate endDate, int page, int size) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return getFilteredRecords(currentUserId, preset, startDate, endDate, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    @LoggableAction("EXPORTED_ATTENDANCE")
    public byte[] exportAttendanceToExcel(DateRangePreset preset, LocalDate startDate, LocalDate endDate, UUID userId) {
        DateRangeRequest request = DateRangeRequest.builder()
                .preset(preset)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        DateRangeResolver.DateRange range = DateRangeResolver.resolve(request);

        var spec = AttendanceSpecification.filterRecords(userId, range.startDate(), range.endDate());
        List<Attendance> records = attendanceRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "date"));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Report");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            // Create Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Employee Name", "Email", "Login Time", "Logout Time", "Total Active Hours"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIdx = 1;
            for (Attendance record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getDate().toString());
                row.createCell(1).setCellValue(record.getUser().getName());
                row.createCell(2).setCellValue(record.getUser().getEmail());
                row.createCell(3).setCellValue(record.getLoginTime().format(formatter));
                row.createCell(4).setCellValue(record.getLogoutTime() != null ? record.getLogoutTime().format(formatter) : "-");
                row.createCell(5).setCellValue(formatDecimalHours(record.getTotalHours()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generating Excel report", e);
            throw new BusinessException("Failed to generate Excel report");
        }
    }

    private String formatDecimalHours(Double hours) {
        if (hours == null) return "00:00";
        long h = hours.longValue();
        long m = Math.round((hours - h) * 60);
        return String.format("%02d:%02d", h, m);
    }

    private double calculateHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0.0;
        long millis = java.time.temporal.ChronoUnit.MILLIS.between(start, end);
        return millis / 3600000.0; // 3,600,000 milliseconds in an hour
    }
}
