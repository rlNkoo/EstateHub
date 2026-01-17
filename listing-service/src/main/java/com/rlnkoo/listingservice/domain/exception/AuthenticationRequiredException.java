package com.rlnkoo.listingservice.domain.exception;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}