package com.gridauthority.coordinator.domain.exceptions;

public abstract class KmsException extends RuntimeException {

    protected KmsException(String message) {
        super(message);
    }

    protected KmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
