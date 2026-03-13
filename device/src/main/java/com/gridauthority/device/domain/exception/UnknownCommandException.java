
package com.gridauthority.device.domain.exception;

public class UnknownCommandException extends DeviceException {

    public UnknownCommandException(String action) {
        super("Unknown command: " + action);
    }
}