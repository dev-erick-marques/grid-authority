package com.gridauthority.device.domain.exception;

public class DeviceAlreadyActiveException extends RuntimeException {
    public DeviceAlreadyActiveException(String deviceId) {
        super("Device " + deviceId + " is already active");
    }
}