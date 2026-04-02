package com.jobhunt.tracker.module.reminder.scheduler;

import com.jobhunt.tracker.config.mail.EmailService;
import com.jobhunt.tracker.module.auth.repository.OtpTokenRepository;
import com.jobhunt.tracker.module.auth.repository.RefreshTokenRepository;
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
    private final OtpTokenRepository otpTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.reminder.cron}")
    @Transactional
    public void sendManualReminders() {
        List<Reminder> pending = reminderRepository.findPendingReminders(LocalDateTime.now());

        if (pending.isEmpty()) {
            log.debug("No pending manual reminders");
            return;
        }

        log.info("Processing {} manual reminder(s)", pending.size());

        for (Reminder reminder : pending) {
            try {
                JobApplication job = reminder.getJob();

                // ── Fix: check emailNotifications setting ──
                boolean emailEnabled = userSettingsRepository
                        .findByUserId(job.getUser().getId())
                        .map(UserSettings::getEmailNotifications)
                        .orElse(true);

                if (!emailEnabled) {
                    reminder.setIsSent(true);
                    reminderRepository.save(reminder);
                    continue;
                }

                // ── Fix: mark sent TRƯỚC khi gửi → nếu email fail thì log,
                //    nhưng không gửi duplicate. Chấp nhận trade-off này.
                //    Nếu cần guarantee delivery → dùng outbox pattern.
                reminder.setIsSent(true);
                reminderRepository.save(reminder);

                emailService.sendReminderEmail(
                        job.getUser().getEmail(),
                        job.getUser().getFullName(),
                        job.getPosition(),
                        reminder.getMessage()
                );

                log.info("Manual reminder sent to: {} for job: {}",
                        job.getUser().getEmail(), job.getPosition());

            } catch (Exception e) {
                log.error("Failed to process manual reminder id={}: {}",
                        reminder.getId(), e.getMessage());
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

        for (JobApplication job : staleJobs) {
            try {
                UserSettings settings = userSettingsRepository
                        .findByUserId(job.getUser().getId())
                        .orElse(null);

                // ── Fix #14: check reminderEnabled ──
                boolean reminderEnabled = settings != null
                        ? settings.getReminderEnabled()
                        : true;

                if (!reminderEnabled) continue;

                // ── Fix: check emailNotifications ──
                boolean emailEnabled = settings != null
                        ? settings.getEmailNotifications()
                        : true;

                if (!emailEnabled) continue;

                int reminderAfterDays = settings != null
                        ? settings.getReminderAfterDays()
                        : 7;

                if (job.getUpdatedAt().isBefore(
                        LocalDateTime.now().minusDays(reminderAfterDays))) {

                    emailService.sendAutoReminderEmail(
                            job.getUser().getEmail(),
                            job.getUser().getFullName(),
                            job.getPosition(),
                            reminderAfterDays
                    );

                    log.info("Auto reminder sent to: {} for job: {}",
                            job.getUser().getEmail(), job.getPosition());
                }

            } catch (Exception e) {
                log.error("Failed to send auto reminder for job id={}: {}",
                        job.getId(), e.getMessage());
            }
        }
    }

    // ── Fix #8: cleanup expired tokens ──
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        otpTokenRepository.deleteExpiredAndUsed();
        refreshTokenRepository.deleteExpiredAndRevoked();
        log.info("Expired OTP tokens and refresh tokens cleaned up");
    }
}