package com.kalibyte.architect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Metadata carried in the JSON part of the multipart submission request.
 * Binary file content is received separately via {@code @RequestPart("files")}.
 */
@Data
public class TaskSubmitRequest {

    @NotBlank(message = "Submission notes are required")
    private String submissionNotes;

    @NotNull(message = "Hours invested is required")
    @Positive(message = "Hours invested must be positive")
    private Double hoursInvested;
}
