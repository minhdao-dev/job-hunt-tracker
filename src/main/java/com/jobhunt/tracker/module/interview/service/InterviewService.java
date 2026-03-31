package com.jobhunt.tracker.module.interview.service;

import com.jobhunt.tracker.module.interview.dto.*;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewResponse create(UUID userId, UUID jobId, CreateInterviewRequest request);

    List<InterviewResponse> getAll(UUID userId, UUID jobId);

    InterviewResponse getById(UUID userId, UUID jobId, UUID interviewId);

    InterviewResponse update(UUID userId, UUID jobId, UUID interviewId, UpdateInterviewRequest request);

    InterviewResponse updateResult(UUID userId, UUID jobId, UUID interviewId, UpdateInterviewResultRequest request);

    void delete(UUID userId, UUID jobId, UUID interviewId);
}