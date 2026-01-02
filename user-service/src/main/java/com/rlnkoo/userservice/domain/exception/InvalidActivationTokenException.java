package com.rlnkoo.userservice.domain.exception;

public class InvalidActivationTokenException extends RuntimeException {
    public InvalidActivationTokenException(String message) {
        super(message);
    }
}
