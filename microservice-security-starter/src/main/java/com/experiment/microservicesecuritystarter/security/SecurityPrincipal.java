package com.experiment.microservicesecuritystarter.security;

public record SecurityPrincipal(
        String userId,
        String email,
        SecurityRole role
) {
}
