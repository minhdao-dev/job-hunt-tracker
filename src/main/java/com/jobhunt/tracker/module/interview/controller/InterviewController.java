package com.jobhunt.tracker.module.interview.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.interview.dto.*;
import com.jobhunt.tracker.module.interview.service.InterviewService;
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
@RequestMapping("/api/v1/jobs/{jobId}/interviews")
@RequiredArgsConstructor
@Tag(name = "Interview", description = "Interview management endpoints")
public class InterviewController {

    private final InterviewService interviewService;
    private final JwtService jwtService;

    @PostMapping
    @Operation(summary = "Create a new interview for a job application")
    public ResponseEntity<AppResponse<InterviewResponse>> create(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateInterviewRequest body,
            HttpServletRequest request) {

        InterviewResponse response = interviewService.create(extractUserId(request), jobId, body);
        return ResponseEntity
                .status(201)
                .body(AppResponse.created("Interview created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all interviews for a job application")
    public ResponseEntity<AppResponse<List<InterviewResponse>>> getAll(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        List<InterviewResponse> response = interviewService.getAll(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @GetMapping("/{interviewId}")
    @Operation(summary = "Get interview by ID")
    public ResponseEntity<AppResponse<InterviewResponse>> getById(
            @PathVariable UUID jobId,
            @PathVariable UUID interviewId,
            HttpServletRequest request) {

        InterviewResponse response = interviewService.getById(
                extractUserId(request), jobId, interviewId
        );
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @PutMapping("/{interviewId}")
    @Operation(summary = "Update interview details")
    public ResponseEntity<AppResponse<InterviewResponse>> update(
            @PathVariable UUID jobId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewRequest body,
            HttpServletRequest request) {

        InterviewResponse response = interviewService.update(
                extractUserId(request), jobId, interviewId, body
        );
        return ResponseEntity.ok(AppResponse.success("Interview updated successfully", response));
    }

    @PatchMapping("/{interviewId}/result")
    @Operation(summary = "Update interview result and notes")
    public ResponseEntity<AppResponse<InterviewResponse>> updateResult(
            @PathVariable UUID jobId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewResultRequest body,
            HttpServletRequest request) {

        InterviewResponse response = interviewService.updateResult(
                extractUserId(request), jobId, interviewId, body
        );
        return ResponseEntity.ok(AppResponse.success("Interview result updated", response));
    }

    @DeleteMapping("/{interviewId}")
    @Operation(summary = "Delete interview (soft delete)")
    public ResponseEntity<AppResponse<Void>> delete(
            @PathVariable UUID jobId,
            @PathVariable UUID interviewId,
            HttpServletRequest request) {

        interviewService.delete(extractUserId(request), jobId, interviewId);
        return ResponseEntity.ok(AppResponse.success("Interview deleted successfully"));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}