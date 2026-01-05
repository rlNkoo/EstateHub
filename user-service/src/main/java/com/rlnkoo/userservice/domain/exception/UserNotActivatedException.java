package com.rlnkoo.userservice.domain.exception;

public class UserNotActivatedException extends DomainException {
    public UserNotActivatedException() {
        super("User account is not activated");
    }
}
