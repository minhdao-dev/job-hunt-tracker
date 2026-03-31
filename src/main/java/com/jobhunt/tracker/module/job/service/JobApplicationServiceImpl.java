package com.jobhunt.tracker.module.job.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.auth.entity.User;
import com.jobhunt.tracker.module.auth.repository.UserRepository;
import com.jobhunt.tracker.module.company.entity.Company;
import com.jobhunt.tracker.module.company.repository.CompanyRepository;
import com.jobhunt.tracker.module.job.dto.*;
import com.jobhunt.tracker.module.job.entity.*;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.job.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobRepository;
    private final StatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public JobApplicationResponse create(UUID userId, CreateJobApplicationRequest request) {
        User user = findActiveUser(userId);

        Company company = resolveCompany(userId, request.companyId());

        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMin() > request.salaryMax()) {
            throw AppException.badRequest("Salary min must not exceed salary max");
        }

        JobApplication job = JobApplication.builder()
                .user(user)
                .company(company)
                .position(request.position())
                .jobUrl(request.jobUrl())
                .jobDescription(request.jobDescription())
                .appliedDate(request.appliedDate() != null
                        ? request.appliedDate() : LocalDate.now())
                .source(request.source() != null ? request.source() : JobSource.OTHER)
                .status(JobStatus.APPLIED)
                .priority(request.priority() != null ? request.priority() : JobPriority.MEDIUM)
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .currency(request.currency() != null ? request.currency() : "VND")
                .isRemote(request.isRemote() != null ? request.isRemote() : false)
                .notes(request.notes())
                .build();

        job = jobRepository.save(job);

        recordStatusHistory(job, null, JobStatus.APPLIED, "Initial application");

        log.info("Job application created: {} for user: {}", job.getPosition(), user.getEmail());

        return toResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getAll(UUID userId, JobStatus status,
                                               String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return jobRepository
                    .searchByKeyword(userId, keyword.trim(), pageable)
                    .map(this::toResponse);
        }

        if (status != null) {
            return jobRepository
                    .findAllByUserIdAndStatus(userId, status, pageable)
                    .map(this::toResponse);
        }

        return jobRepository
                .findAllByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getById(UUID userId, UUID jobId) {
        return toResponse(findJob(userId, jobId));
    }

    @Override
    @Transactional
    public JobApplicationResponse update(UUID userId, UUID jobId,
                                         UpdateJobApplicationRequest request) {
        JobApplication job = findJob(userId, jobId);

        Company company = resolveCompany(userId, request.companyId());

        if (request.salaryMin() != null && request.salaryMax() != null
                && request.salaryMin() > request.salaryMax()) {
            throw AppException.badRequest("Salary min must not exceed salary max");
        }

        job.setCompany(company);
        job.setPosition(request.position());
        job.setJobUrl(request.jobUrl());
        job.setJobDescription(request.jobDescription());
        if (request.appliedDate() != null) job.setAppliedDate(request.appliedDate());
        if (request.source() != null) job.setSource(request.source());
        if (request.priority() != null) job.setPriority(request.priority());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        if (request.currency() != null) job.setCurrency(request.currency());
        if (request.isRemote() != null) job.setIsRemote(request.isRemote());
        job.setNotes(request.notes());

        job = jobRepository.save(job);

        log.info("Job application updated: {} for user: {}", job.getPosition(), userId);

        return toResponse(job);
    }

    @Override
    @Transactional
    public JobApplicationResponse updateStatus(UUID userId, UUID jobId,
                                               UpdateStatusRequest request) {
        JobApplication job = findJob(userId, jobId);

        if (job.getStatus() == request.status()) {
            throw AppException.badRequest(
                    "Job is already in status: " + request.status()
            );
        }

        JobStatus oldStatus = job.getStatus();
        job.setStatus(request.status());
        job = jobRepository.save(job);

        recordStatusHistory(job, oldStatus, request.status(), request.note());

        log.info("Job status updated: {} → {} for job: {}",
                oldStatus, request.status(), jobId);

        return toResponse(job);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID jobId) {
        JobApplication job = findJob(userId, jobId);
        job.softDelete();
        jobRepository.save(job);
        log.info("Job application soft-deleted: {} for user: {}", jobId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(UUID userId, UUID jobId) {
        findJob(userId, jobId);
        return historyRepository.findAllByJobId(jobId)
                .stream()
                .map(h -> new StatusHistoryResponse(
                        h.getId(),
                        h.getOldStatus(),
                        h.getNewStatus(),
                        h.getNote(),
                        h.getChangedAt()
                ))
                .toList();
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private JobApplication findJob(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> AppException.notFound("Job application not found: " + jobId));
    }

    private Company resolveCompany(UUID userId, UUID companyId) {
        if (companyId == null) return null;
        return companyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> AppException.notFound("Company not found: " + companyId));
    }

    private void recordStatusHistory(JobApplication job, JobStatus oldStatus,
                                     JobStatus newStatus, String note) {
        StatusHistory history = StatusHistory.builder()
                .job(job)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .build();
        historyRepository.save(history);
    }

    private JobApplicationResponse toResponse(JobApplication job) {
        return new JobApplicationResponse(
                job.getId(),
                job.getCompany() != null ? job.getCompany().getId() : null,
                job.getCompany() != null ? job.getCompany().getName() : null,
                job.getPosition(),
                job.getJobUrl(),
                job.getJobDescription(),
                job.getAppliedDate(),
                job.getSource(),
                job.getStatus(),
                job.getPriority(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getIsRemote(),
                job.getNotes(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}