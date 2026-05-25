package com.kalibyte.architect.attendance.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private LocalDate date;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private Double totalHours;
    private String exceptionNote;
    private Boolean isManuallyEdited;
}
