
package com.labresource.platform.common.security;

import com.labresource.platform.user.entity.User;
import com.labresource.platform.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            // Validate and extract identity from JWT.
            String email = jwtService.extractEmail(token);

            if (email == null || email.isBlank()) {
                sendUnauthorized(response, "Invalid JWT identity");
                return;
            }

            /*
             * If a Bearer token is supplied, do not silently reuse a
             * different user's session authentication for this request.
             */
            SecurityContextHolder.clearContext();

            User user = userRepository.findByEmailWithRole(email)
                    .orElse(null);

            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                sendUnauthorized(response, "User not found or inactive");
                return;
            }

            if (user.getRole() == null
                    || user.getRole().getName() == null
                    || user.getRole().getName().isBlank()) {
                sendForbidden(response, "User role is not configured");
                return;
            }

            String roleName = user.getRole().getName()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            // Avoid ROLE_ROLE_MANAGER if the database already stores ROLE_.
            if (roleName.startsWith("ROLE_")) {
                roleName = roleName.substring(5);
            }

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + roleName)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            System.out.println("JWT request: "
                    + request.getMethod() + " " + request.getRequestURI());
            System.out.println("JWT authenticated email: " + email);
            System.out.println("JWT user role: " + roleName);
            System.out.println("JWT authorities: " + authorities);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            sendUnauthorized(response, "Invalid or expired JWT token");
        }
    }

    private void sendUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"" + message + "\"}"
        );
    }

    private void sendForbidden(
            HttpServletResponse response,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"" + message + "\"}"
        );
    }
}
