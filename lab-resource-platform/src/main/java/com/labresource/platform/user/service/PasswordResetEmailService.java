
package com.labresource.platform.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PasswordResetEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetEmail(String email, String rawToken) {

        String resetLink = frontendUrl
                + "/reset-password?token="
                + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Lab Resource Platform - Password Reset");

        message.setText(
                "Hello,\n\n"
                        + "We received a request to reset your password.\n\n"
                        + "Click the link below to reset your password:\n"
                        + resetLink + "\n\n"
                        + "This link will expire in 30 minutes.\n"
                        + "If you did not request this, please ignore this email.\n\n"
                        + "Lab Resource Platform Team"
        );

        mailSender.send(message);
    }
}
