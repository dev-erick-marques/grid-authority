package com.gridauthority.device.domain.exception;

public class DeviceAlreadyShutdownException extends DeviceException {
    public DeviceAlreadyShutdownException(String deviceId) {
        super("Device " + deviceId + " is already shutdown");
    }
}