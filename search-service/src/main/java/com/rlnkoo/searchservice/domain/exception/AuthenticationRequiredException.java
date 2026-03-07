package com.rlnkoo.searchservice.domain.exception;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}