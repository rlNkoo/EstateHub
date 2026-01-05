package com.rlnkoo.userservice.domain.exception;

public class EmailAlreadyUsedException extends DomainException {

    public EmailAlreadyUsedException(String email) {
        super("Email already in use: " + email);
    }
}
