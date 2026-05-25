package com.kalibyte.architect.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskReviewRequest {
    
    @NotNull(message = "Approval status is required")
    private Boolean approved;
    
    @NotBlank(message = "Review comment is required")
    private String reviewComment;
}
