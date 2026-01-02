package com.rlnkoo.userservice.domain.model;

public enum Role {
    USER,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}