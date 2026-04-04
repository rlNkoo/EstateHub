package com.rlnkoo.userservice.api.me;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeControllerIT {

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
    void shouldReturnUnauthorizedWhenGettingMeWithoutToken() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserProfileWhenTokenIsValid() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("me@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER, Role.ADMIN))
                .firstName("Jan")
                .lastName("Kowalski")
                .phoneNumber("123456789")
                .build();

        UserEntity savedUser = userRepository.save(user);
        String token = generateAccessToken(savedUser);

        // when + then
        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "ADMIN")))
                .andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.phoneNumber").value("123456789"));
    }

    @Test
    void shouldUpdateCurrentUserProfileWhenTokenIsValid() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("update@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .firstName("Old")
                .lastName("Name")
                .phoneNumber("111111111")
                .build();

        UserEntity savedUser = userRepository.save(user);
        String token = generateAccessToken(savedUser);

        String requestBody = """
                {
                  "firstName": "Anna",
                  "lastName": "Nowak",
                  "phoneNumber": "987654321"
                }
                """;

        // when + then
        mockMvc.perform(put("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("update@example.com"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER")))
                .andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.lastName").value("Nowak"))
                .andExpect(jsonPath("$.phoneNumber").value("987654321"));

        UserEntity updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Anna", updatedUser.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("Nowak", updatedUser.getLastName());
        org.junit.jupiter.api.Assertions.assertEquals("987654321", updatedUser.getPhoneNumber());
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRequestIsInvalid() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("invalidupdate@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        UserEntity savedUser = userRepository.save(user);
        String token = generateAccessToken(savedUser);

        String tooLongFirstName = "a".repeat(101);

        String requestBody = """
        {
          "firstName": "%s",
          "lastName": "Nowak",
          "phoneNumber": "987654321"
        }
        """.formatted(tooLongFirstName);

        // when + then
        mockMvc.perform(put("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/me"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUpdatingMeWithoutToken() throws Exception {
        // given
        String requestBody = """
                {
                  "firstName": "Anna",
                  "lastName": "Nowak",
                  "phoneNumber": "987654321"
                }
                """;

        // when + then
        mockMvc.perform(put("/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserDoesNotExist() throws Exception {
        // given
        UUID missingUserId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(
                missingUserId,
                "missing@example.com",
                Set.of(Role.USER)
        );

        // when + then
        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: " + missingUserId))
                .andExpect(jsonPath("$.path").value("/me"));
    }

    private String generateAccessToken(UserEntity user) {
        return jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRoles()
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}