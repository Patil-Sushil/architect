package com.kalibyte.architect.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceExceptionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Login time is required")
    private LocalDateTime loginTime;

    private LocalDateTime logoutTime;

    @NotNull(message = "Exception note is required")
    private String exceptionNote;
}
