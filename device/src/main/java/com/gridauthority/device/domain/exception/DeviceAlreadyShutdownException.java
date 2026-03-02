package com.gridauthority.device.domain.exception;

public class DeviceAlreadyShutdownException extends RuntimeException {
    public DeviceAlreadyShutdownException(String deviceId) {
        super("Device " + deviceId + " is already shutdown");
    }
}