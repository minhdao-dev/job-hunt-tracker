package com.jobhunt.tracker.config.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailService {

    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("verifyUrl", frontendUrl + "/verify-email?token=" + token);

        String content = templateEngine.process("email/verify-email", context);
        sendEmail(toEmail, "Xác nhận email - Job Hunt Tracker", content);
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String fullName, String token) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + token);

        String content = templateEngine.process("email/reset-password", context);
        sendEmail(toEmail, "Đặt lại mật khẩu - Job Hunt Tracker", content);
    }

    @Async
    public void sendReminderEmail(String toEmail, String fullName,
                                  String position, String message) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("position", position);
        context.setVariable("message", message != null ? message : "Đừng quên follow up nhé!");

        String content = templateEngine.process("email/reminder", context);
        sendEmail(toEmail, "Nhắc nhở - " + position + " | Job Hunt Tracker", content);
    }

    @Async
    public void sendAutoReminderEmail(String toEmail, String fullName,
                                      String position, int daysSinceUpdate) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("position", position);
        context.setVariable("daysSinceUpdate", daysSinceUpdate);

        String content = templateEngine.process("email/auto-reminder", context);
        sendEmail(toEmail, "Cập nhật tiến độ - " + position + " | Job Hunt Tracker", content);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            Email from = new Email(fromEmail);
            Email toAddress = new Email(to);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toAddress, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error: status={}, body={}", response.getStatusCode(), response.getBody());
            } else {
                log.info("Email sent to: {} (status={})", to, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}