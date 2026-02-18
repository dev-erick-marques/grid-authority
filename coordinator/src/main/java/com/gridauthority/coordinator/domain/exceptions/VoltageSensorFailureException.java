package com.gridauthority.coordinator.domain.exceptions;

public class VoltageSensorFailureException extends VoltageException {
    public VoltageSensorFailureException(String message) {
        super(message);
    }
}
