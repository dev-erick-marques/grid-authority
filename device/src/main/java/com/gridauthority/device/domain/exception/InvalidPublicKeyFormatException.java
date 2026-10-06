package com.gridauthority.device.domain.exception;

public class InvalidPublicKeyFormatException extends CommandVerificationException {

    public InvalidPublicKeyFormatException(String message, Throwable cause) {
        super(message, cause);
    }
    public InvalidPublicKeyFormatException(String message) {
        super(message);
    }
}