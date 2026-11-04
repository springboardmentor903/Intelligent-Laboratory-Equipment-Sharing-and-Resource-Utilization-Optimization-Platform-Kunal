
package com.labresource.platform.common.security;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes = oauthUser.getAttributes();

        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new ServletException("Google account email not found");
        }

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() ->
                        new ServletException("User account not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ServletException("User account is inactive");
        }

        String roleName = user.getRole() != null
                ? user.getRole().getName()
                : "UNKNOWN";

        // Generate JWT using existing JwtService
        String token = jwtService.generateToken(user.getEmail());

        // Encode data for frontend callback URL fragment
        String encodedEmail = URLEncoder.encode(
                user.getEmail(), StandardCharsets.UTF_8
        );

        String encodedName = URLEncoder.encode(
                user.getFullName() == null ? "" : user.getFullName(),
                StandardCharsets.UTF_8
        );

        String encodedRole = URLEncoder.encode(
                roleName, StandardCharsets.UTF_8
        );

        /*
         * Token is placed in URL fragment (#), not query parameters.
         * URL fragments are not sent in normal HTTP requests.
         */
        String redirectUrl = frontendUrl
                + "/oauth2/callback"
                + "#token=" + token
                + "&email=" + encodedEmail
                + "&name=" + encodedName
                + "&role=" + encodedRole;

        response.sendRedirect(redirectUrl);
    }
}
