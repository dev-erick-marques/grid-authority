package com.gridauthority.device.domain.exception;

public abstract class CommandVerificationException extends RuntimeException {

    protected CommandVerificationException(String message) {
        super(message);
    }

    protected CommandVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}