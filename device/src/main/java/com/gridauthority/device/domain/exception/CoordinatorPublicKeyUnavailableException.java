package com.gridauthority.device.domain.exception;

public class CoordinatorPublicKeyUnavailableException extends CommandVerificationException {

    public CoordinatorPublicKeyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoordinatorPublicKeyUnavailableException(String message) {
        super(message);
    }
}