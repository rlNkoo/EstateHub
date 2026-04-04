package com.rlnkoo.userservice.api.admin;

import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnUnauthorizedWhenChangingRolesWithoutToken() throws Exception {
        // given
        UUID targetUserId = UUID.randomUUID();

        String requestBody = """
                {
                  "roles": ["ADMIN", "USER"]
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenAuthenticatedUserIsNotAdmin() throws Exception {
        // given
        UserEntity targetUser = UserEntity.builder()
                .email("target@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        UserEntity savedTargetUser = userRepository.save(targetUser);

        String userToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "regular-user@example.com",
                Set.of(Role.USER)
        );

        String requestBody = """
                {
                  "roles": ["ADMIN", "USER"]
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", savedTargetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path")
                        .value("/admin/users/" + savedTargetUser.getId() + "/roles"));
    }

    @Test
    void shouldChangeUserRolesWhenAuthenticatedUserIsAdmin() throws Exception {
        // given
        UserEntity targetUser = UserEntity.builder()
                .email("target@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        UserEntity savedTargetUser = userRepository.save(targetUser);

        String adminToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "admin@example.com",
                Set.of(Role.ADMIN)
        );

        String requestBody = """
                {
                  "roles": ["ADMIN", "USER"]
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", savedTargetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedTargetUser.getId().toString()))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ADMIN", "USER")))
                .andExpect(jsonPath("$.message").value("Roles updated successfully"));

        UserEntity updatedUser = userRepository.findById(savedTargetUser.getId()).orElseThrow();
        assertEquals(Set.of(Role.ADMIN, Role.USER), updatedUser.getRoles());
    }

    @Test
    void shouldReturnBadRequestWhenRoleIsInvalid() throws Exception {
        // given
        UserEntity targetUser = UserEntity.builder()
                .email("target@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        UserEntity savedTargetUser = userRepository.save(targetUser);

        String adminToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "admin@example.com",
                Set.of(Role.ADMIN)
        );

        String requestBody = """
                {
                  "roles": ["USER", "SUPERADMIN"]
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", savedTargetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid role: SUPERADMIN"))
                .andExpect(jsonPath("$.path")
                        .value("/admin/users/" + savedTargetUser.getId() + "/roles"));
    }

    @Test
    void shouldReturnNotFoundWhenTargetUserDoesNotExist() throws Exception {
        // given
        UUID missingUserId = UUID.randomUUID();

        String adminToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "admin@example.com",
                Set.of(Role.ADMIN)
        );

        String requestBody = """
                {
                  "roles": ["ADMIN", "USER"]
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", missingUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: " + missingUserId))
                .andExpect(jsonPath("$.path")
                        .value("/admin/users/" + missingUserId + "/roles"));
    }

    @Test
    void shouldReturnBadRequestWhenRolesRequestIsEmpty() throws Exception {
        // given
        UserEntity targetUser = UserEntity.builder()
                .email("target@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        UserEntity savedTargetUser = userRepository.save(targetUser);

        String adminToken = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "admin@example.com",
                Set.of(Role.ADMIN)
        );

        String requestBody = """
                {
                  "roles": []
                }
                """;

        // when + then
        mockMvc.perform(put("/admin/users/{userId}/roles", savedTargetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path")
                        .value("/admin/users/" + savedTargetUser.getId() + "/roles"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}