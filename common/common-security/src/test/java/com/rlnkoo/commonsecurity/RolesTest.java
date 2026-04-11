package com.rlnkoo.commonsecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolesTest {

    @Test
    void shouldConvertRoleToSpringAuthority() {
        String result = Roles.asAuthority("ADMIN");

        assertEquals("ROLE_ADMIN", result);
    }
}