package com.rlnkoo.userservice.domain.exception;

public class InvalidRoleException extends DomainException {

    public InvalidRoleException(String role) {
        super("Invalid role: " + role);
    }
}