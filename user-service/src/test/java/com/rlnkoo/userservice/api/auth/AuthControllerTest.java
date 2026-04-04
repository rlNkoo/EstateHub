package com.rlnkoo.userservice.api.auth;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.PasswordResetCompletedPayload;
import com.rlnkoo.userservice.events.types.PasswordResetRequestedPayload;
import com.rlnkoo.userservice.events.types.UserActivatedPayload;
import com.rlnkoo.userservice.events.types.UserRegisteredPayload;
import com.rlnkoo.userservice.persistence.entity.ActivationTokenEntity;
import com.rlnkoo.userservice.persistence.entity.PasswordResetTokenEntity;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.ActivationTokenRepository;
import com.rlnkoo.userservice.persistence.repository.PasswordResetTokenRepository;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.rlnkoo.userservice.domain.model.Role.USER;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UserEventsPublisher userEventsPublisher;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        activationTokenRepository.deleteAll();
        userRepository.deleteAll();
        reset(userEventsPublisher);
    }

    @Test
    void shouldRegisterUser() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "NewUser@Example.com",
                  "password": "Password123!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Registration successful. Please confirm your email address."))
                .andExpect(jsonPath("$.activationRequired").value(true));

        UserEntity savedUser = userRepository.findByEmailIgnoreCase("newuser@example.com")
                .orElseThrow();

        assertEquals("newuser@example.com", savedUser.getEmail());
        assertFalse(savedUser.isEnabled());
        assertEquals(Set.of(USER), savedUser.getRoles());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPasswordHash()));

        List<ActivationTokenEntity> tokens = activationTokenRepository.findAll();
        assertEquals(1, tokens.size());
        assertEquals(savedUser.getId(), tokens.getFirst().getUserId());
        assertNotNull(tokens.getFirst().getTokenHash());
        assertNotNull(tokens.getFirst().getExpiresAt());
        assertNull(tokens.getFirst().getUsedAt());

        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), eventCaptor.capture());

        EventEnvelope<?> event = eventCaptor.getValue();
        assertEquals("UserRegisteredV1", event.eventType());

        UserRegisteredPayload payload = (UserRegisteredPayload) event.payload();
        assertEquals(savedUser.getId(), payload.userId());
        assertEquals("newuser@example.com", payload.email());
        assertNotNull(payload.activationToken());

        String expectedHash = tokenService.sha256Hex(payload.activationToken());
        assertEquals(expectedHash, tokens.getFirst().getTokenHash());
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyUsed() throws Exception {
        // given
        UserEntity existingUser = UserEntity.builder()
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(false)
                .roles(Set.of(USER))
                .build();

        userRepository.save(existingUser);

        String requestBody = """
                {
                  "email": "EXISTING@example.com",
                  "password": "Password123!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already in use: EXISTING@example.com"))
                .andExpect(jsonPath("$.path").value("/auth/register"));

        assertEquals(1, userRepository.count());
        assertEquals(0, activationTokenRepository.count());
    }

    @Test
    void shouldReturnBadRequestWhenRegisterRequestIsInvalid() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "not-an-email",
                  "password": "123"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
        assertEquals(0, activationTokenRepository.count());
    }

    @Test
    void shouldConfirmRegistrationAndActivateUser() throws Exception {
        // given
        String registerRequest = """
                {
                  "email": "activate@example.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isCreated());

        UserEntity savedUser = userRepository.findByEmailIgnoreCase("activate@example.com")
                .orElseThrow();

        ArgumentCaptor<EventEnvelope> registerEventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), registerEventCaptor.capture());

        UserRegisteredPayload registerPayload =
                (UserRegisteredPayload) registerEventCaptor.getValue().payload();

        String plainToken = registerPayload.activationToken();
        assertNotNull(plainToken);

        clearInvocations(userEventsPublisher);

        String confirmRequest = """
                {
                  "token": "%s"
                }
                """.formatted(plainToken);

        // when + then
        mockMvc.perform(post("/auth/confirm-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Account activated successfully. You can now log in."));

        UserEntity activatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertTrue(activatedUser.isEnabled());
        assertNotNull(activatedUser.getConfirmedAt());

        ActivationTokenEntity tokenEntity = activationTokenRepository.findAll().getFirst();
        assertNotNull(tokenEntity.getUsedAt());

        ArgumentCaptor<EventEnvelope> activatedEventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), activatedEventCaptor.capture());

        EventEnvelope<?> activatedEvent = activatedEventCaptor.getValue();
        assertEquals("UserActivatedV1", activatedEvent.eventType());

        UserActivatedPayload activatedPayload = (UserActivatedPayload) activatedEvent.payload();
        assertEquals(savedUser.getId(), activatedPayload.userId());
        assertEquals("activate@example.com", activatedPayload.email());
        assertNotNull(activatedPayload.activatedAt());
    }

    @Test
    void shouldReturnBadRequestWhenActivationTokenIsInvalid() throws Exception {
        // given
        String requestBody = """
                {
                  "token": "invalid-token"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/confirm-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid or already used activation token"))
                .andExpect(jsonPath("$.path").value("/auth/confirm-registration"));
    }

    @Test
    void shouldLoginUserWhenCredentialsAreValid() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("login@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(Set.of(USER))
                .build();

        userRepository.save(user);

        String requestBody = """
                {
                  "email": "login@example.com",
                  "password": "Password123!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void shouldReturnForbiddenWhenUserIsNotActivated() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("inactive@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(false)
                .roles(Set.of(USER))
                .build();

        userRepository.save(user);

        String requestBody = """
                {
                  "email": "inactive@example.com",
                  "password": "Password123!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User account is not activated"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsInvalid() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("wrongpass@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(Set.of(USER))
                .build();

        userRepository.save(user);

        String requestBody = """
                {
                  "email": "wrongpass@example.com",
                  "password": "BadPassword999!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    void shouldRequestPasswordResetAndCreateTokenWhenUserExists() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("reset@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(Set.of(USER))
                .build();

        UserEntity savedUser = userRepository.save(user);

        String requestBody = """
                {
                  "email": "reset@example.com"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a password reset link has been sent."));

        List<PasswordResetTokenEntity> tokens = passwordResetTokenRepository.findAll();
        assertEquals(1, tokens.size());
        assertEquals(savedUser.getId(), tokens.getFirst().getUserId());
        assertNotNull(tokens.getFirst().getTokenHash());
        assertNotNull(tokens.getFirst().getExpiresAt());
        assertNull(tokens.getFirst().getUsedAt());

        ArgumentCaptor<EventEnvelope> eventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), eventCaptor.capture());

        EventEnvelope<?> event = eventCaptor.getValue();
        assertEquals("PasswordResetRequestedV1", event.eventType());

        PasswordResetRequestedPayload payload = (PasswordResetRequestedPayload) event.payload();
        assertEquals(savedUser.getId(), payload.userId());
        assertEquals("reset@example.com", payload.email());
        assertNotNull(payload.resetToken());

        String expectedHash = tokenService.sha256Hex(payload.resetToken());
        assertEquals(expectedHash, tokens.getFirst().getTokenHash());
    }

    @Test
    void shouldReturnOkAndDoNothingWhenPasswordResetRequestedForUnknownEmail() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "unknown@example.com"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for this email, a password reset link has been sent."));

        assertEquals(0, passwordResetTokenRepository.count());
    }

    @Test
    void shouldConfirmPasswordResetAndChangePassword() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("confirmreset@example.com")
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .enabled(true)
                .roles(Set.of(USER))
                .build();

        UserEntity savedUser = userRepository.save(user);

        String requestResetBody = """
                {
                  "email": "confirmreset@example.com"
                }
                """;

        mockMvc.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestResetBody))
                .andExpect(status().isOk());

        ArgumentCaptor<EventEnvelope> requestEventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), requestEventCaptor.capture());

        PasswordResetRequestedPayload requestPayload =
                (PasswordResetRequestedPayload) requestEventCaptor.getValue().payload();

        String plainToken = requestPayload.resetToken();
        assertNotNull(plainToken);

        clearInvocations(userEventsPublisher);

        String confirmBody = """
                {
                  "token": "%s",
                  "newPassword": "NewPassword123!"
                }
                """.formatted(plainToken);

        // when + then
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Password has been reset successfully. You can now log in."));

        UserEntity updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPassword123!", updatedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPassword123!", updatedUser.getPasswordHash()));

        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository.findAll().getFirst();
        assertNotNull(tokenEntity.getUsedAt());

        ArgumentCaptor<EventEnvelope> completedEventCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(userEventsPublisher).publish(eq(savedUser.getId()), completedEventCaptor.capture());

        EventEnvelope<?> completedEvent = completedEventCaptor.getValue();
        assertEquals("PasswordResetCompletedV1", completedEvent.eventType());

        PasswordResetCompletedPayload completedPayload =
                (PasswordResetCompletedPayload) completedEvent.payload();

        assertEquals(savedUser.getId(), completedPayload.userId());
        assertEquals("confirmreset@example.com", completedPayload.email());
        assertNotNull(completedPayload.completedAt());
    }

    @Test
    void shouldReturnBadRequestWhenPasswordResetTokenIsInvalid() throws Exception {
        // given
        String requestBody = """
                {
                  "token": "invalid-reset-token",
                  "newPassword": "NewPassword123!"
                }
                """;

        // when + then
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or already used password reset token"))
                .andExpect(jsonPath("$.path").value("/auth/password-reset/confirm"));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordResetTokenIsExpired() throws Exception {
        // given
        UserEntity user = UserEntity.builder()
                .email("expiredreset@example.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(Set.of(USER))
                .build();

        UserEntity savedUser = userRepository.save(user);

        String plainToken = "expired-plain-token";
        String tokenHash = tokenService.sha256Hex(plainToken);

        PasswordResetTokenEntity expiredToken = PasswordResetTokenEntity.builder()
                .userId(savedUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        passwordResetTokenRepository.save(expiredToken);

        String requestBody = """
                {
                  "token": "%s",
                  "newPassword": "NewPassword123!"
                }
                """.formatted(plainToken);

        // when + then
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Password reset token has expired"))
                .andExpect(jsonPath("$.path").value("/auth/password-reset/confirm"));
    }
}