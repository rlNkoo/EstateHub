package com.rlnkoo.mediaservice.domain.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String serviceName, String message) {
        super(serviceName + " error: " + message);
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(serviceName + " error: " + message, cause);
    }
}