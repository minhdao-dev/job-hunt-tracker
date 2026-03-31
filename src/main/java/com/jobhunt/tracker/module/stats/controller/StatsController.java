package com.jobhunt.tracker.module.stats.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.stats.dto.*;
import com.jobhunt.tracker.module.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Statistics endpoints")
public class StatsController {

    private final StatsService statsService;
    private final JwtService jwtService;

    @GetMapping("/overview")
    @Operation(summary = "Get overall statistics")
    public ResponseEntity<AppResponse<OverviewStatsResponse>> getOverview(
            HttpServletRequest request) {

        return ResponseEntity.ok(
                AppResponse.success(statsService.getOverview(extractUserId(request)))
        );
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get job application statistics")
    public ResponseEntity<AppResponse<JobStatsResponse>> getJobStats(
            HttpServletRequest request) {

        return ResponseEntity.ok(
                AppResponse.success(statsService.getJobStats(extractUserId(request)))
        );
    }

    @GetMapping("/interviews")
    @Operation(summary = "Get interview statistics")
    public ResponseEntity<AppResponse<InterviewStatsResponse>> getInterviewStats(
            HttpServletRequest request) {

        return ResponseEntity.ok(
                AppResponse.success(statsService.getInterviewStats(extractUserId(request)))
        );
    }

    @GetMapping("/offers")
    @Operation(summary = "Get offer statistics")
    public ResponseEntity<AppResponse<OfferStatsResponse>> getOfferStats(
            HttpServletRequest request) {

        return ResponseEntity.ok(
                AppResponse.success(statsService.getOfferStats(extractUserId(request)))
        );
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}