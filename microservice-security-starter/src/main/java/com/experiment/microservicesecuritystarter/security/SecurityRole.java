package com.experiment.microservicesecuritystarter.security;

import java.util.Locale;

public enum SecurityRole {
    CUSTOMER,
    SELLER;

    public static SecurityRole from(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Role cannot be null or blank"
            );
        }

        try {
            return valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role: " + value
            );
        }
    }
}
