package com.experiment.microservicesecuritystarter.exception;

public class SecurityUnauthorizedException extends RuntimeException {
    public SecurityUnauthorizedException(String message) {
        super(message);
    }
}
