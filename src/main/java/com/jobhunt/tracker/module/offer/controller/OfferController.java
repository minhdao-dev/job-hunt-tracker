package com.jobhunt.tracker.module.offer.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.offer.dto.*;
import com.jobhunt.tracker.module.offer.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/offer")
@RequiredArgsConstructor
@Tag(name = "Offer", description = "Offer management endpoints")
public class OfferController {

    private final OfferService offerService;
    private final JwtService jwtService;

    @PostMapping
    @Operation(summary = "Create offer for a job application")
    public ResponseEntity<AppResponse<OfferResponse>> create(
            @PathVariable UUID jobId,
            @Valid @RequestBody CreateOfferRequest body,
            HttpServletRequest request) {

        OfferResponse response = offerService.create(extractUserId(request), jobId, body);
        return ResponseEntity
                .status(201)
                .body(AppResponse.created("Offer created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get offer of a job application")
    public ResponseEntity<AppResponse<OfferResponse>> get(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        OfferResponse response = offerService.get(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @PutMapping
    @Operation(summary = "Update offer details")
    public ResponseEntity<AppResponse<OfferResponse>> update(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateOfferRequest body,
            HttpServletRequest request) {

        OfferResponse response = offerService.update(extractUserId(request), jobId, body);
        return ResponseEntity.ok(AppResponse.success("Offer updated successfully", response));
    }

    @PatchMapping("/decision")
    @Operation(summary = "Update offer decision")
    public ResponseEntity<AppResponse<OfferResponse>> updateDecision(
            @PathVariable UUID jobId,
            @Valid @RequestBody UpdateOfferDecisionRequest body,
            HttpServletRequest request) {

        OfferResponse response = offerService.updateDecision(extractUserId(request), jobId, body);
        return ResponseEntity.ok(AppResponse.success("Offer decision updated", response));
    }

    @DeleteMapping
    @Operation(summary = "Delete offer (soft delete)")
    public ResponseEntity<AppResponse<Void>> delete(
            @PathVariable UUID jobId,
            HttpServletRequest request) {

        offerService.delete(extractUserId(request), jobId);
        return ResponseEntity.ok(AppResponse.success("Offer deleted successfully"));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}