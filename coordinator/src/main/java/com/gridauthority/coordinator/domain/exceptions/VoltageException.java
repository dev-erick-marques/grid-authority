package com.gridauthority.coordinator.domain.exceptions;

public abstract class VoltageException extends RuntimeException {
    protected VoltageException(String message) {
        super(message);
    }
}
