package com.jobhunt.tracker.config.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail,
                                      String fullName,
                                      String token) {

        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("verifyUrl",
                frontendUrl + "/verify-email?token=" + token);

        String content = templateEngine.process(
                "email/verify-email", context
        );

        sendHtmlEmail(
                toEmail,
                "Xác nhận email - Job Hunt Tracker",
                content
        );
    }

    @Async
    public void sendResetPasswordEmail(String toEmail,
                                       String fullName,
                                       String token) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("resetUrl",
                frontendUrl + "/reset-password?token=" + token);

        String content = templateEngine.process(
                "email/reset-password", context
        );

        sendHtmlEmail(
                toEmail,
                "Đặt lại mật khẩu - Job Hunt Tracker",
                content
        );
    }

    @Async
    public void sendReminderEmail(String toEmail, String fullName,
                                  String position, String message) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("position", position);
        context.setVariable("message", message != null ? message : "Đừng quên follow up nhé!");

        String content = templateEngine.process("email/reminder", context);

        sendHtmlEmail(toEmail, "Nhắc nhở - " + position + " | Job Hunt Tracker", content);
    }

    @Async
    public void sendAutoReminderEmail(String toEmail, String fullName,
                                      String position, int daysSinceUpdate) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("position", position);
        context.setVariable("daysSinceUpdate", daysSinceUpdate);

        String content = templateEngine.process("email/auto-reminder", context);

        sendHtmlEmail(toEmail, "Cập nhật tiến độ - " + position + " | Job Hunt Tracker", content);
    }

    private void sendHtmlEmail(String to,
                               String subject,
                               String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8"
            );
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}