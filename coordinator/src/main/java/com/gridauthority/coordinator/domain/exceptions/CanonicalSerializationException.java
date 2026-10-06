package com.gridauthority.coordinator.domain.exceptions;

public class CanonicalSerializationException extends KmsException {

    public CanonicalSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}