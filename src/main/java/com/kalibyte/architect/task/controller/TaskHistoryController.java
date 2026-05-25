package com.kalibyte.architect.task.controller;

import com.kalibyte.architect.common.response.ApiResponse;
import com.kalibyte.architect.common.response.PageResponse;
import com.kalibyte.architect.task.dto.response.TaskHistoryResponse;
import com.kalibyte.architect.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/task-history")
@RequiredArgsConstructor
public class TaskHistoryController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<TaskHistoryResponse>>> getGlobalHistory(
            @RequestParam(required = false) UUID taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getGlobalTaskHistory(taskId, page, size)));
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<PageResponse<TaskHistoryResponse>>> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getMyTaskHistory(page, size)));
    }
}
