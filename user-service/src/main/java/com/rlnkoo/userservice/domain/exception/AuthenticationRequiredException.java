package com.rlnkoo.userservice.domain.exception;

public class AuthenticationRequiredException extends DomainException {
    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}