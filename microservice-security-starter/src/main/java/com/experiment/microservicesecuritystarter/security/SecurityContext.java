package com.experiment.microservicesecuritystarter.security;

import com.experiment.microservicesecuritystarter.exception.SecurityUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public class SecurityContext {

    private final HttpServletRequest request;

    public SecurityContext(HttpServletRequest request) {
        this.request = request;
    }

    public SecurityPrincipal get() {

        return (SecurityPrincipal) request.getAttribute(
                SecurityContextFilter.SECURITY_PRINCIPAL_ATTRIBUTE
        );
    }

    public SecurityPrincipal require() {

        SecurityPrincipal principal = get();

        if (principal == null) {
            throw new SecurityUnauthorizedException(
                    "Authentication required"
            );
        }

        return principal;
    }

    public boolean isAuthenticated() {
        return get() != null;
    }
}