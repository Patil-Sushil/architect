package com.kalibyte.architect.task.dto.response;

import com.kalibyte.architect.task.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistoryResponse {
    private UUID id;
    private TaskSummary task;
    private TaskStatus fromStatus;
    private TaskStatus toStatus;
    private TaskResponse.UserSummary changedBy;
    private String remarks;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummary {
        private UUID id;
        private String taskName;
        private String jobNumber;
    }
}
