package com.rlnkoo.mediaservice.domain.exception;

public class StorageOperationException extends RuntimeException {

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}