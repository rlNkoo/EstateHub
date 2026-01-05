package com.rlnkoo.userservice.domain.exception;

public class InvalidActivationTokenException extends DomainException {
    public InvalidActivationTokenException() {
        super("Invalid or already used activation token");
    }
}
