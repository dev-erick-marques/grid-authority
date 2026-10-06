package com.gridauthority.coordinator.domain.exceptions;

public class KmsUnavailableException extends KmsException {

    public KmsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}