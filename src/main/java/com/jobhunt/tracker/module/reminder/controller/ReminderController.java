package com.jobhunt.tracker.module.reminder.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.reminder.dto.*;
import com.jobhunt.tracker.module.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminder", description = "Reminder management endpoints")
public class ReminderController {

    private final ReminderService reminderService;
    private final JwtService jwtService;

    @PostMapping
    @Operation(summary = "Create a reminder for a job application")
    public ResponseEntity<AppResponse<ReminderResponse>> create(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateReminderRequest body,
            HttpServletRequest request) {

        ReminderResponse response = reminderService.create(extractUserId(request), jobId, body);
        return ResponseEntity
                .status(201)
                .body(AppResponse.created("Reminder created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all reminders for a job application")
    public ResponseEntity<AppResponse<List<ReminderResponse>>> getAll(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        List<ReminderResponse> response = reminderService.getAll(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @PutMapping("/{reminderId}")
    @Operation(summary = "Update a reminder")
    public ResponseEntity<AppResponse<ReminderResponse>> update(
            @PathVariable UUID jobId,
            @PathVariable UUID reminderId,
            @Valid @RequestBody UpdateReminderRequest body,
            HttpServletRequest request) {

        ReminderResponse response = reminderService.update(
                extractUserId(request), jobId, reminderId, body
        );
        return ResponseEntity.ok(AppResponse.success("Reminder updated successfully", response));
    }

    @DeleteMapping("/{reminderId}")
    @Operation(summary = "Delete a reminder (soft delete)")
    public ResponseEntity<AppResponse<Void>> delete(
            @PathVariable UUID jobId,
            @PathVariable UUID reminderId,
            HttpServletRequest request) {

        reminderService.delete(extractUserId(request), jobId, reminderId);
        return ResponseEntity.ok(AppResponse.success("Reminder deleted successfully"));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}