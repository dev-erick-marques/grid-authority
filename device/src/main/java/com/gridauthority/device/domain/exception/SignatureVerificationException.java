package com.gridauthority.device.domain.exception;

public class SignatureVerificationException extends CommandVerificationException {

    public SignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}