package com.gridauthority.coordinator.domain.exceptions;

public class InsufficientVoltageSamplesException extends VoltageException {
    public InsufficientVoltageSamplesException(String message) {
        super(message);
    }
}
