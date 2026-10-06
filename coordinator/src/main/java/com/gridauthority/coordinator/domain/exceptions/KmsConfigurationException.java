package com.gridauthority.coordinator.domain.exceptions;

public class KmsConfigurationException extends KmsException {

    public KmsConfigurationException(String message) {
        super(message);
    }

    public KmsConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}