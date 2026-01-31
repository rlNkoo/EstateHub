package com.rlnkoo.mediaservice.domain.exception;

public class ValidationFailedException extends RuntimeException {

    public ValidationFailedException(String message) {
        super(message);
    }
}