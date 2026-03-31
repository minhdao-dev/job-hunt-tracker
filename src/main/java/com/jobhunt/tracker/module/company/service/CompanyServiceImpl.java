package com.jobhunt.tracker.module.company.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.company.dto.*;
import com.jobhunt.tracker.module.company.entity.Company;
import com.jobhunt.tracker.module.company.entity.CompanySize;
import com.jobhunt.tracker.module.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CompanyResponse create(UUID userId, CreateCompanyRequest request) {
        User user = findActiveUser(userId);

        if (companyRepository.existsByUserIdAndName(userId, request.name())) {
            throw AppException.conflict(
                    "Company with name '" + request.name() + "' already exists"
            );
        }

        Company company = Company.builder()
                .user(user)
                .name(request.name())
                .website(request.website())
                .industry(request.industry())
                .size(request.size() != null ? request.size() : CompanySize.UNKNOWN)
                .location(request.location())
                .isOutsource(request.isOutsource() != null ? request.isOutsource() : false)
                .notes(request.notes())
                .build();

        company = companyRepository.save(company);

        log.info("Company created: {} for user: {}", company.getName(), user.getEmail());

        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAll(UUID userId, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return companyRepository
                    .searchByName(userId, keyword.trim(), pageable)
                    .map(this::toResponse);
        }
        return companyRepository
                .findAllByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getById(UUID userId, UUID companyId) {
        return toResponse(findCompany(userId, companyId));
    }

    @Override
    @Transactional
    public CompanyResponse update(UUID userId, UUID companyId, UpdateCompanyRequest request) {
        Company company = findCompany(userId, companyId);

        if (companyRepository.existsByUserIdAndNameExcluding(userId, request.name(), companyId)) {
            throw AppException.conflict(
                    "Company with name '" + request.name() + "' already exists"
            );
        }

        company.setName(request.name());
        company.setWebsite(request.website());
        company.setIndustry(request.industry());
        company.setSize(request.size() != null ? request.size() : CompanySize.UNKNOWN);
        company.setLocation(request.location());
        company.setIsOutsource(request.isOutsource() != null ? request.isOutsource() : false);
        company.setNotes(request.notes());

        company = companyRepository.save(company);

        log.info("Company updated: {} for user: {}", company.getName(), userId);

        return toResponse(company);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID companyId) {
        Company company = findCompany(userId, companyId);

        company.softDelete();
        companyRepository.save(company);

        log.info("Company soft-deleted: {} for user: {}", company.getName(), userId);
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private Company findCompany(UUID userId, UUID companyId) {
        return companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> AppException.notFound(
                        "Company not found: " + companyId
                ));
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                company.getSize(),
                company.getLocation(),
                company.getIsOutsource(),
                company.getNotes(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}