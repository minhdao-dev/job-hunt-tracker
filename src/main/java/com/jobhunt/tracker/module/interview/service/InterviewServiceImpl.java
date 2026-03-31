package com.jobhunt.tracker.module.interview.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.interview.dto.CreateInterviewRequest;
import com.jobhunt.tracker.module.interview.dto.InterviewResponse;
import com.jobhunt.tracker.module.interview.dto.UpdateInterviewRequest;
import com.jobhunt.tracker.module.interview.dto.UpdateInterviewResultRequest;
import com.jobhunt.tracker.module.interview.entity.Interview;
import com.jobhunt.tracker.module.interview.entity.InterviewType;
import com.jobhunt.tracker.module.interview.repository.InterviewRepository;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobRepository;

    @Override
    @Transactional
    public InterviewResponse create(UUID userId, UUID jobId, CreateInterviewRequest request) {
        JobApplication job = findJob(userId, jobId);

        Interview interview = Interview.builder()
                .job(job)
                .contactId(request.contactId())
                .round(request.round() != null ? request.round() : 1)
                .interviewType(request.interviewType() != null
                        ? request.interviewType() : InterviewType.TECHNICAL)
                .scheduledAt(request.scheduledAt())
                .durationMinutes(request.durationMinutes() != null
                        ? request.durationMinutes() : 60)
                .location(request.location())
                .preparationNote(request.preparationNote())
                .build();

        interview = interviewRepository.save(interview);

        log.info("Interview created: round {} for job: {}", interview.getRound(), jobId);

        return toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getAll(UUID userId, UUID jobId) {
        findJob(userId, jobId);
        return interviewRepository.findAllByJobIdAndUserId(jobId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getById(UUID userId, UUID jobId, UUID interviewId) {
        return toResponse(findInterview(userId, jobId, interviewId));
    }

    @Override
    @Transactional
    public InterviewResponse update(UUID userId, UUID jobId, UUID interviewId,
                                    UpdateInterviewRequest request) {
        Interview interview = findInterview(userId, jobId, interviewId);

        if (request.round() != null) interview.setRound(request.round());
        if (request.interviewType() != null) interview.setInterviewType(request.interviewType());
        interview.setScheduledAt(request.scheduledAt());
        if (request.durationMinutes() != null) interview.setDurationMinutes(request.durationMinutes());
        interview.setLocation(request.location());
        interview.setContactId(request.contactId());
        interview.setPreparationNote(request.preparationNote());
        interview.setQuestionsAsked(request.questionsAsked());
        interview.setMyAnswers(request.myAnswers());
        interview.setFeedback(request.feedback());

        interview = interviewRepository.save(interview);

        log.info("Interview updated: {} for job: {}", interviewId, jobId);

        return toResponse(interview);
    }

    @Override
    @Transactional
    public InterviewResponse updateResult(UUID userId, UUID jobId, UUID interviewId,
                                          UpdateInterviewResultRequest request) {
        Interview interview = findInterview(userId, jobId, interviewId);

        interview.setResult(request.result());
        if (request.questionsAsked() != null) interview.setQuestionsAsked(request.questionsAsked());
        if (request.myAnswers() != null) interview.setMyAnswers(request.myAnswers());
        if (request.feedback() != null) interview.setFeedback(request.feedback());

        interview = interviewRepository.save(interview);

        log.info("Interview result updated: {} → {} for interview: {}",
                interviewId, request.result(), interviewId);

        return toResponse(interview);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID jobId, UUID interviewId) {
        Interview interview = findInterview(userId, jobId, interviewId);
        interview.softDelete();
        interviewRepository.save(interview);
        log.info("Interview soft-deleted: {} for job: {}", interviewId, jobId);
    }

    private JobApplication findJob(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> AppException.notFound("Job application not found: " + jobId));
    }

    private Interview findInterview(UUID userId, UUID jobId, UUID interviewId) {
        return interviewRepository.findByIdAndJobIdAndUserId(interviewId, jobId, userId)
                .orElseThrow(() -> AppException.notFound("Interview not found: " + interviewId));
    }

    private InterviewResponse toResponse(Interview i) {
        return new InterviewResponse(
                i.getId(),
                i.getJob().getId(),
                i.getContactId(),
                i.getRound(),
                i.getInterviewType(),
                i.getScheduledAt(),
                i.getDurationMinutes(),
                i.getLocation(),
                i.getResult(),
                i.getPreparationNote(),
                i.getQuestionsAsked(),
                i.getMyAnswers(),
                i.getFeedback(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}