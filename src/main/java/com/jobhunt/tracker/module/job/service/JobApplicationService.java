package com.jobhunt.tracker.module.job.service;

import com.jobhunt.tracker.module.job.dto.*;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobApplicationService {

    JobApplicationResponse create(UUID userId, CreateJobApplicationRequest request);

    Page<JobApplicationResponse> getAll(UUID userId, JobStatus status, String keyword, Pageable pageable);

    JobApplicationResponse getById(UUID userId, UUID jobId);

    JobApplicationResponse update(UUID userId, UUID jobId, UpdateJobApplicationRequest request);

    JobApplicationResponse updateStatus(UUID userId, UUID jobId, UpdateStatusRequest request);

    void delete(UUID userId, UUID jobId);

    List<StatusHistoryResponse> getStatusHistory(UUID userId, UUID jobId);
}