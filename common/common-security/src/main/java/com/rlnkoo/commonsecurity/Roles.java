package com.rlnkoo.commonsecurity;

public final class Roles {

    private Roles() {}
    public static String asAuthority(String role) {
        return "ROLE_" + role;
    }
}