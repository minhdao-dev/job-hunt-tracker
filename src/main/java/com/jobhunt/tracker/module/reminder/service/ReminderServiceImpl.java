package com.jobhunt.tracker.module.reminder.service;

import com.jobhunt.tracker.common.exception.AppException;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.reminder.dto.*;
import com.jobhunt.tracker.module.reminder.entity.Reminder;
import com.jobhunt.tracker.module.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final JobApplicationRepository jobRepository;

    @Override
    @Transactional
    public ReminderResponse create(UUID userId, UUID jobId, CreateReminderRequest request) {
        JobApplication job = findJob(userId, jobId);

        Reminder reminder = Reminder.builder()
                .job(job)
                .remindAt(request.remindAt())
                .message(request.message())
                .build();

        reminder = reminderRepository.save(reminder);

        log.info("Reminder created for job: {} at {}", jobId, request.remindAt());

        return toResponse(reminder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> getAll(UUID userId, UUID jobId) {
        findJob(userId, jobId);
        return reminderRepository.findAllByJobIdAndUserId(jobId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReminderResponse update(UUID userId, UUID jobId, UUID reminderId,
                                   UpdateReminderRequest request) {
        Reminder reminder = findReminder(userId, jobId, reminderId);

        reminder.setRemindAt(request.remindAt());
        reminder.setMessage(request.message());
        reminder.setIsSent(false);

        reminder = reminderRepository.save(reminder);

        log.info("Reminder updated: {} for job: {}", reminderId, jobId);

        return toResponse(reminder);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID jobId, UUID reminderId) {
        Reminder reminder = findReminder(userId, jobId, reminderId);
        reminder.softDelete();
        reminderRepository.save(reminder);
        log.info("Reminder soft-deleted: {} for job: {}", reminderId, jobId);
    }

    private JobApplication findJob(UUID userId, UUID jobId) {
        return jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> AppException.notFound("Job application not found: " + jobId));
    }

    private Reminder findReminder(UUID userId, UUID jobId, UUID reminderId) {
        return reminderRepository.findByIdAndJobIdAndUserId(reminderId, jobId, userId)
                .orElseThrow(() -> AppException.notFound("Reminder not found: " + reminderId));
    }

    private ReminderResponse toResponse(Reminder r) {
        return new ReminderResponse(
                r.getId(),
                r.getJob().getId(),
                r.getJob().getPosition(),
                r.getMessage(),
                r.getRemindAt(),
                r.getIsSent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}