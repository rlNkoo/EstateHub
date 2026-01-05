package com.rlnkoo.userservice.domain.exception;

public class ActivationTokenExpiredException extends DomainException {

    public ActivationTokenExpiredException() {
        super("Activation token has expired");
    }
}
