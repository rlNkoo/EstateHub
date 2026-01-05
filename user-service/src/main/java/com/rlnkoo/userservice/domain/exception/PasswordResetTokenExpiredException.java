package com.rlnkoo.userservice.domain.exception;

public class PasswordResetTokenExpiredException extends DomainException {
    public PasswordResetTokenExpiredException() {
        super("Password reset token has expired");
    }
}
