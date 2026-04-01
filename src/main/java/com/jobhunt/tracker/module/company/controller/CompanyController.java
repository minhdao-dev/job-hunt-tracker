package com.jobhunt.tracker.module.company.controller;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.common.response.AppResponse;
import com.jobhunt.tracker.config.security.JwtService;
import com.jobhunt.tracker.module.company.dto.*;
import com.jobhunt.tracker.module.company.service.CompanyService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company", description = "Company management endpoints")
public class CompanyController {

    private final CompanyService companyService;
    private final JwtService jwtService;

    @PostMapping
    @Operation(summary = "Create a new company")
    public ResponseEntity<AppResponse<CompanyResponse>> create(
            @Valid @RequestBody CreateCompanyRequest body,
            HttpServletRequest request) {

        CompanyResponse response = companyService.create(extractUserId(request), body);
        return ResponseEntity
                .status(201)
                .body(AppResponse.created("Company created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all companies (paginated, searchable)")
    public ResponseEntity<AppResponse<java.util.List<CompanyResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CompanyResponse> result = companyService.getAll(
                extractUserId(request), keyword, pageable
        );

        return ResponseEntity.ok(AppResponse.paginated(result));
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Get company by ID")
    public ResponseEntity<AppResponse<CompanyResponse>> getById(
            @PathVariable UUID companyId,
            HttpServletRequest request) {

        CompanyResponse response = companyService.getById(extractUserId(request), companyId);
        return ResponseEntity.ok(AppResponse.success(response));
    }

    @PutMapping("/{companyId}")
    @Operation(summary = "Update company")
    public ResponseEntity<AppResponse<CompanyResponse>> update(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest body,
            HttpServletRequest request) {

        CompanyResponse response = companyService.update(
                extractUserId(request), companyId, body
        );
        return ResponseEntity.ok(AppResponse.success("Company updated successfully", response));
    }

    @DeleteMapping("/{companyId}")
    @Operation(summary = "Delete company (soft delete)")
    public ResponseEntity<AppResponse<Void>> delete(
            @PathVariable UUID companyId,
            HttpServletRequest request) {

        companyService.delete(extractUserId(request), companyId);
        return ResponseEntity.ok(AppResponse.success("Company deleted successfully"));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return jwtService.extractUserId(header.substring(7));
        }
        throw AppException.unauthorized("Access token not found");
    }
}