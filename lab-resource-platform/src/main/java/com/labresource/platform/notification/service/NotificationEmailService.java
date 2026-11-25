
package com.labresource.platform.notification.service;

import com.labresource.platform.notification.entity.Notification;
import com.labresource.platform.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public NotificationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNotificationEmail(
            User user,
            Notification notification
    ) {
        if (user == null || user.getEmail() == null
                || user.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email address is missing"
            );
        }

        String link = buildNotificationLink(notification);

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(senderEmail);
        email.setTo(user.getEmail());
        email.setSubject(notification.getTitle());

        email.setText(
                notification.getMessage()
                        + "\n\nOpen in Lab Resource Platform:\n"
                        + link
                        + "\n\nThis is an automated notification."
        );

        mailSender.send(email);
    }

    private String buildNotificationLink(Notification notification) {
        String base = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;

        return base + "/notifications";
    }
}
