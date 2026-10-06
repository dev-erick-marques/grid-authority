package com.gridauthority.coordinator.domain.exceptions;

public class KmsSigningFailedException extends KmsException {

    public KmsSigningFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}