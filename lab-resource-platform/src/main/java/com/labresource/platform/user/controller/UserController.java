
package com.labresource.platform.user.controller;

import com.labresource.platform.common.security.JwtService;

import com.labresource.platform.user.dto.LoginRequest;
import com.labresource.platform.user.dto.LoginResponse;
import com.labresource.platform.user.dto.UserProfileUpdateRequest;
import com.labresource.platform.user.dto.UserRegistrationRequest;
import com.labresource.platform.user.dto.UserResponse;
import com.labresource.platform.user.dto.ForgotPasswordRequest;
import com.labresource.platform.user.dto.ResetPasswordRequest;

import com.labresource.platform.user.entity.User;

import com.labresource.platform.user.service.UserService;
import com.labresource.platform.user.service.PasswordResetService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public UserController(
            UserService userService,
            JwtService jwtService,
            PasswordResetService passwordResetService) {

        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers()
                .stream()
                .map(UserResponse::new)
                .toList();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        User user = userService.createUser(request);
        return new UserResponse(user);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        User user = userService.login(request);
        String token = jwtService.generateToken(user.getEmail());

        return new LoginResponse(token, new UserResponse(user));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyProfile(Authentication authentication) {
        User user = userService.getMyProfile(authentication.getName());
        return new UserResponse(user);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        User user = userService.updateMyProfile(
                authentication.getName(),
                request
        );

        return new UserResponse(user);
    }

    @GetMapping("/my-role")
    @PreAuthorize("isAuthenticated()")
    public String getMyRole(Authentication authentication) {
        return authentication.getAuthorities().toString();
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String adminTest() {
        return "SYSTEM_ADMIN access granted!";
    }

    @GetMapping("/lab-manager-test")
    @PreAuthorize("hasRole('LAB_MANAGER')")
    public String labManagerTest() {
        return "LAB_MANAGER access granted!";
    }

    // Forgot Password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestPasswordReset(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If an account exists for this email, password reset instructions will be sent."
                )
        );
    }

    // Reset Password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset successfully."
                )
        );
    }
}
