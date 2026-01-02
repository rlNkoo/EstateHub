package com.rlnkoo.userservice.domain.exception;

public class UserNotActivatedException extends RuntimeException {
    public UserNotActivatedException(String message) {
        super(message);
    }
}
