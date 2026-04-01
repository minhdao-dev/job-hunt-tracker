package com.jobhunt.tracker.module.job.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.job.dto.*;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Application", description = "Job application management endpoints")
public class JobApplicationController {

    private final JobApplicationService jobService;
    private final JwtService jwtService;

    @PostMapping
    @Operation(summary = "Create a new job application")
    public ResponseEntity<AppResponse<JobApplicationResponse>> create(
            @Valid @RequestBody CreateJobApplicationRequest body,
            HttpServletRequest request) {

        JobApplicationResponse response = jobService.create(extractUserId(request), body);
        return ResponseEntity
                .status(201)
                .body(AppResponse.created("Job application created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all job applications (paginated, filterable)")
    public ResponseEntity<AppResponse<List<JobApplicationResponse>>> getAll(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("appliedDate").descending()
        );
        Page<JobApplicationResponse> result = jobService.getAll(
                extractUserId(request), status, keyword, pageable
        );

        return ResponseEntity.ok(AppResponse.paginated(result));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job application by ID")
    public ResponseEntity<AppResponse<JobApplicationResponse>> getById(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        JobApplicationResponse response = jobService.getById(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update job application")
    public ResponseEntity<AppResponse<JobApplicationResponse>> update(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateJobApplicationRequest body,
            HttpServletRequest request) {

        JobApplicationResponse response = jobService.update(
                extractUserId(request), jobId, body
        );
        return ResponseEntity.ok(AppResponse.success("Job application updated successfully", response));
    }

    @PatchMapping("/{jobId}/status")
    @Operation(summary = "Update job application status")
    public ResponseEntity<AppResponse<JobApplicationResponse>> updateStatus(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateStatusRequest body,
            HttpServletRequest request) {

        JobApplicationResponse response = jobService.updateStatus(
                extractUserId(request), jobId, body
        );
        return ResponseEntity.ok(AppResponse.success("Status updated successfully", response));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete job application (soft delete)")
    public ResponseEntity<AppResponse<Void>> delete(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        jobService.delete(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success("Job application deleted successfully"));
    }

    @GetMapping("/{jobId}/status-history")
    @Operation(summary = "Get status change history of a job application")
    public ResponseEntity<AppResponse<List<StatusHistoryResponse>>> getStatusHistory(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        List<StatusHistoryResponse> history = jobService.getStatusHistory(
                extractUserId(request), jobId
        );
        return ResponseEntity.ok(AppResponse.success(history));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}