
package com.labresource.platform.user.service;

import com.labresource.platform.user.dto.ForgotPasswordRequest;
import com.labresource.platform.user.dto.ResetPasswordRequest;
import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenService tokenService;
    private final PasswordResetEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenService tokenService,
            PasswordResetEmailService emailService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void requestPasswordReset(ForgotPasswordRequest request) {

        String email = request.email().trim();

        userRepository.findByEmail(email).ifPresent(user -> {

            // Google-only accounts cannot use local password reset.
            if (user.getPasswordHash() == null) {
                return;
            }

            String rawToken = tokenService.createToken(user);

            emailService.sendResetEmail(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        String newPassword = request.newPassword();

        User user = tokenService.consumeToken(request.token());

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }
}
