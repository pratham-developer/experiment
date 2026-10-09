package com.experiment.orderservice.advice;

import com.experiment.microservicesecuritystarter.exception.SecurityForbiddenException;
import com.experiment.microservicesecuritystarter.exception.SecurityUnauthorizedException;
import com.experiment.orderservice.exception.TransientInventoryException;
import feign.FeignException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(SecurityUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            SecurityUnauthorizedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(SecurityForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            SecurityForbiddenException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.FORBIDDEN.value(),
                        HttpStatus.FORBIDDEN.getReasonPhrase(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        message
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(TransientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInventoryUnavailable(
            TransientInventoryException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                        "Inventory service is temporarily unavailable"
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        "Order conflicts with existing data"
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Invalid request parameter"
                ));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex
    ) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || status.is5xxServerError()) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = "Downstream service request failed";
        try {
            JsonNode body = objectMapper.readTree(ex.contentUTF8());
            JsonNode messageNode = body.get("message");

            if (messageNode != null && !messageNode.isNull()) {
                message = messageNode.asString();
            }
        } catch (Exception ignored) {
            // Keep the fallback message.
        }
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected error occurred"
                ));
    }
}