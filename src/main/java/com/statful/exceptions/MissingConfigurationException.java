package com.statful.exceptions;

public class MissingConfigurationException extends RuntimeException {
    public MissingConfigurationException(String message) {
        super(message);
    }
}
