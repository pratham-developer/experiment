package com.experiment.microservicesecuritystarter.exception;

public class SecurityForbiddenException extends RuntimeException {
    public SecurityForbiddenException(String message) {
        super(message);
    }
}
