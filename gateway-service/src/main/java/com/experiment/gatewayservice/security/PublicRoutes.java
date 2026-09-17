package com.experiment.gatewayservice.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PublicRoutes {

    private static final Set<String> PUBLIC_ROUTES = Set.of(
            "/api/users/auth/login",
            "/api/users/auth/register"
    );

    public boolean isPublic(String path) {
        return PUBLIC_ROUTES.contains(path);
    }
}