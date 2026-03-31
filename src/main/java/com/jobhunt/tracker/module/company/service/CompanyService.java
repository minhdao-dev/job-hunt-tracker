package com.jobhunt.tracker.module.company.service;

import com.jobhunt.tracker.module.company.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CompanyService {

    CompanyResponse create(UUID userId, CreateCompanyRequest request);

    Page<CompanyResponse> getAll(UUID userId, String keyword, Pageable pageable);

    CompanyResponse getById(UUID userId, UUID companyId);

    CompanyResponse update(UUID userId, UUID companyId, UpdateCompanyRequest request);

    void delete(UUID userId, UUID companyId);
}