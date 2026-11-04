
package com.labresource.platform.user.service;

import com.labresource.platform.user.entity.PasswordResetToken;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.PasswordResetTokenRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetTokenService(
            PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String createToken(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(
                Instant.now().plus(30, ChronoUnit.MINUTES)
        );

        tokenRepository.save(resetToken);

        return rawToken;
    }

    @Transactional
    public User consumeToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Reset token is required.");
        }

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid or expired reset token."
                        )
                );

        Instant now = Instant.now();

        if (resetToken.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "Reset token has already been used."
            );
        }

        if (!resetToken.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Reset token has expired."
            );
        }

        resetToken.setUsedAt(now);

        return resetToken.getUser();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to hash reset token.", e
            );
        }
    }
}
