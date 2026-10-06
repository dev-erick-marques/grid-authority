package com.gridauthority.device.domain.exception;

public abstract class DeviceException extends RuntimeException {

    protected DeviceException(String message) {
        super(message);
    }
}