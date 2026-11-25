package com.labresource.platform.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Msg91SmsService {

    @Value("${app.msg91.enabled:false}")
    private boolean enabled;

    @Value("${app.msg91.authkey:}")
    private String authKey;

    @Value("${app.msg91.template-id:}")
    private String templateId;

    @Value("${app.msg91.country-code:91}")
    private String countryCode;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public Msg91SmsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendSms(
            String phone,
            String title,
            String message) {

        if (!enabled) {
            throw new IllegalStateException(
                    "MSG91 SMS integration is disabled");
        }

        if (authKey == null || authKey.isBlank()
                || templateId == null || templateId.isBlank()) {
            throw new IllegalStateException(
                    "MSG91 auth key or template ID is missing");
        }

        String mobile = normalizePhone(phone);

        try {
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("mobiles", mobile);
            recipient.put("VAR1", safeValue(title, 60));
            recipient.put("VAR2", safeValue(message, 300));

            Map<String, Object> payload = new HashMap<>();
            payload.put("template_id", templateId);
            payload.put("short_url", "0");
            payload.put("recipients", List.of(recipient));

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://control.msg91.com/api/v5/flow"))
                    .timeout(Duration.ofSeconds(30))
                    .header("accept", "application/json")
                    .header("authkey", authKey)
                    .header("content-type", "application/json")
                    .POST(BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "MSG91 request failed with HTTP "
                                + response.statusCode()
                                + ": " + response.body());
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "MSG91 SMS request interrupted", ex);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to send SMS through MSG91", ex);
        }
    }

    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException(
                    "User phone number is missing");
        }

        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() == 10) {
            digits = countryCode + digits;
        }

        if (digits.length() < 11 || digits.length() > 15) {
            throw new IllegalArgumentException(
                    "Invalid phone number for MSG91");
        }

        return digits;
    }

    private String safeValue(String value, int maxLength) {

        if (value == null || value.isBlank()) {
            return "Notification";
        }

        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();

        return cleaned.length() > maxLength
                ? cleaned.substring(0, maxLength)
                : cleaned;
    }
}