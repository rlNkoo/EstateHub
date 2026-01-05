package com.rlnkoo.userservice.domain.exception;

public class InvalidPasswordResetTokenException extends DomainException {

    public InvalidPasswordResetTokenException() {
        super("Invalid or already used password reset token");
    }
}
