package com.experiment.microservicesecuritystarter.security;

import com.experiment.microservicesecuritystarter.exception.SecurityForbiddenException;
import com.experiment.microservicesecuritystarter.exception.SecurityUnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SecurityContextFilter extends OncePerRequestFilter {

    public static final String SECURITY_PRINCIPAL_ATTRIBUTE =
            "microservice.security.principal";

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        // No user headers → potentially a public request.
        if (userId == null && email == null && role == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Some headers exist → all three must be present.
        if (userId == null || userId.isBlank()
                || email == null || email.isBlank()
                || role == null || role.isBlank()) {

            throw new SecurityUnauthorizedException(
                    "Incomplete user context"
            );
        }

        SecurityRole securityRole;

        try {
            securityRole = SecurityRole.from(role);
        } catch (IllegalArgumentException e) {
            throw new SecurityForbiddenException(
                    "Invalid user role"
            );
        }

        SecurityPrincipal principal = new SecurityPrincipal(
                userId,
                email,
                securityRole
        );

        request.setAttribute(
                SECURITY_PRINCIPAL_ATTRIBUTE,
                principal
        );

        filterChain.doFilter(request, response);
    }
}