package com.rlnkoo.notificationservice.integration.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.domain.service.UserNotificationService;
import com.rlnkoo.notificationservice.integration.kafka.EventEnvelopeReader;
import com.rlnkoo.notificationservice.integration.user.events.PasswordResetCompletedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.PasswordResetRequestedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.UserActivatedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.UserRegisteredV1Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventsConsumerTest {

    @Mock
    private EventEnvelopeReader reader;

    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private UserEventsConsumer userEventsConsumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldHandleUserRegisteredEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "activationToken": "token-123"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                payloadNode
        );

        UserRegisteredV1Payload payload = new UserRegisteredV1Payload(
                userId,
                "user@example.com",
                "token-123"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, UserRegisteredV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, UserRegisteredV1Payload.class);
        verify(userNotificationService).onUserRegistered(envelope, payload);
        verifyNoMoreInteractions(userNotificationService);
    }

    @Test
    void shouldIgnoreUserRegisteredEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                payloadNode
        );

        UserRegisteredV1Payload payload = new UserRegisteredV1Payload(
                userId,
                "user@example.com",
                null
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, UserRegisteredV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, UserRegisteredV1Payload.class);
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldHandleUserActivatedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant activatedAt = Instant.now();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "activatedAt": "%s"
                }
                """.formatted(userId, activatedAt));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                payloadNode
        );

        UserActivatedV1Payload payload = new UserActivatedV1Payload(
                userId,
                "user@example.com",
                activatedAt
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, UserActivatedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, UserActivatedV1Payload.class);
        verify(userNotificationService).onUserActivated(envelope, payload);
        verifyNoMoreInteractions(userNotificationService);
    }

    @Test
    void shouldIgnoreUserActivatedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                payloadNode
        );

        UserActivatedV1Payload payload = new UserActivatedV1Payload(
                userId,
                "user@example.com",
                null
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, UserActivatedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, UserActivatedV1Payload.class);
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldHandlePasswordResetRequestedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "resetToken": "reset-token-123"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                payloadNode
        );

        PasswordResetRequestedV1Payload payload = new PasswordResetRequestedV1Payload(
                userId,
                "user@example.com",
                "reset-token-123"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, PasswordResetRequestedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, PasswordResetRequestedV1Payload.class);
        verify(userNotificationService).onPasswordResetRequested(envelope, payload);
        verifyNoMoreInteractions(userNotificationService);
    }

    @Test
    void shouldIgnorePasswordResetRequestedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                payloadNode
        );

        PasswordResetRequestedV1Payload payload = new PasswordResetRequestedV1Payload(
                userId,
                "user@example.com",
                null
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, PasswordResetRequestedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, PasswordResetRequestedV1Payload.class);
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldHandlePasswordResetCompletedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant completedAt = Instant.now();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "completedAt": "%s"
                }
                """.formatted(userId, completedAt));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                payloadNode
        );

        PasswordResetCompletedV1Payload payload = new PasswordResetCompletedV1Payload(
                userId,
                "user@example.com",
                completedAt
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, PasswordResetCompletedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, PasswordResetCompletedV1Payload.class);
        verify(userNotificationService).onPasswordResetCompleted(envelope, payload);
        verifyNoMoreInteractions(userNotificationService);
    }

    @Test
    void shouldIgnorePasswordResetCompletedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "userId": "%s",
                  "email": "user@example.com"
                }
                """.formatted(userId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                payloadNode
        );

        PasswordResetCompletedV1Payload payload = new PasswordResetCompletedV1Payload(
                userId,
                "user@example.com",
                null
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, PasswordResetCompletedV1Payload.class)).thenReturn(payload);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, PasswordResetCompletedV1Payload.class);
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldIgnoreUserEventWhenEnvelopeIsMissingRequiredFields() {
        // given
        String message = "message";

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                null,
                "UserRegisteredV1",
                Instant.now(),
                objectMapper.createObjectNode()
        );

        when(reader.read(message)).thenReturn(envelope);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader, never()).readPayload(any(), any());
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldIgnoreUnknownUserEventType() {
        // given
        String message = "message";

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "UnknownEventV1",
                Instant.now(),
                objectMapper.createObjectNode()
        );

        when(reader.read(message)).thenReturn(envelope);

        // when
        userEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader, never()).readPayload(any(), any());
        verifyNoInteractions(userNotificationService);
    }

    @Test
    void shouldRethrowExceptionWhenReaderFailsToDeserializeUserEvent() {
        // given
        String message = "message";
        RuntimeException exception = new RuntimeException("cannot deserialize");

        when(reader.read(message)).thenThrow(exception);

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userEventsConsumer.onMessage(message)
        );

        assertSame(exception, thrown);
        verify(reader).read(message);
        verifyNoInteractions(userNotificationService);
    }
}