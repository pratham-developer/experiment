package com.experiment.orderservice.exception;

public class TransientInventoryException extends RuntimeException {
    public TransientInventoryException(Throwable cause) {
        super(cause);
    }
}