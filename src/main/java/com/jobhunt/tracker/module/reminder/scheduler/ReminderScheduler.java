package com.jobhunt.tracker.module.reminder.scheduler;

import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.module.job.entity.JobApplication;
import com.jobhunt.tracker.module.job.entity.JobStatus;
import com.jobhunt.tracker.module.job.repository.JobApplicationRepository;
import com.jobhunt.tracker.module.reminder.entity.Reminder;
import com.jobhunt.tracker.module.reminder.repository.ReminderRepository;
import com.jobhunt.tracker.module.user.entity.UserSettings;
import com.jobhunt.tracker.module.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderRepository reminderRepository;
    private final JobApplicationRepository jobRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.reminder.cron}")
    @Transactional
    public void sendManualReminders() {
        List<Reminder> pending = reminderRepository.findPendingReminders(LocalDateTime.now());

        if (pending.isEmpty()) {
            log.debug("No pending manual reminders to send");
            return;
        }

        log.info("Sending {} manual reminder(s)", pending.size());

        for (Reminder reminder : pending) {
            try {
                JobApplication job = reminder.getJob();
                String email = job.getUser().getEmail();
                String fullName = job.getUser().getFullName();

                emailService.sendReminderEmail(email, fullName, job.getPosition(), reminder.getMessage());

                reminder.setIsSent(true);
                reminderRepository.save(reminder);

                log.info("Manual reminder sent to: {} for job: {}", email, job.getPosition());
            } catch (Exception e) {
                log.error("Failed to send manual reminder id={}: {}", reminder.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.reminder.cron}")
    @Transactional
    public void sendAutoReminders() {
        List<JobApplication> staleJobs = jobRepository.findStaleJobs(
                List.of(JobStatus.APPLIED, JobStatus.INTERVIEWING)
        );

        if (staleJobs.isEmpty()) {
            log.debug("No stale jobs to remind");
            return;
        }

        log.info("Sending auto reminders for {} stale job(s)", staleJobs.size());

        for (JobApplication job : staleJobs) {
            try {
                int reminderAfterDays = userSettingsRepository
                        .findByUserId(job.getUser().getId())
                        .map(UserSettings::getReminderAfterDays)
                        .orElse(7);

                if (job.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(reminderAfterDays))) {
                    String email = job.getUser().getEmail();
                    String fullName = job.getUser().getFullName();

                    emailService.sendAutoReminderEmail(email, fullName, job.getPosition(), reminderAfterDays);

                    log.info("Auto reminder sent to: {} for job: {}", email, job.getPosition());
                }
            } catch (Exception e) {
                log.error("Failed to send auto reminder for job id={}: {}", job.getId(), e.getMessage());
            }
        }
    }
}