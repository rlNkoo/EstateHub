package com.rlnkoo.notificationservice.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.user.events.UserRegisteredV1Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventEnvelopeReaderTest {

    private EventEnvelopeReader eventEnvelopeReader;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        eventEnvelopeReader = new EventEnvelopeReader(objectMapper);
    }

    @Test
    void shouldDeserializeEventEnvelopeFromJson() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2025-01-10T12:30:00Z");

        String message = """
                {
                  "eventId": "%s",
                  "eventType": "UserRegisteredV1",
                  "occurredAt": "%s",
                  "payload": {
                    "userId": "%s",
                    "email": "user@example.com",
                    "activationToken": "token-123"
                  }
                }
                """.formatted(eventId, occurredAt, userId);

        // when
        EventEnvelope<JsonNode> envelope = eventEnvelopeReader.read(message);

        // then
        assertEquals(eventId, envelope.eventId());
        assertEquals("UserRegisteredV1", envelope.eventType());
        assertEquals(occurredAt, envelope.occurredAt());
        assertNotNull(envelope.payload());
        assertEquals(userId.toString(), envelope.payload().get("userId").asText());
        assertEquals("user@example.com", envelope.payload().get("email").asText());
        assertEquals("token-123", envelope.payload().get("activationToken").asText());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenEnvelopeJsonIsInvalid() {
        // given
        String invalidMessage = "{ invalid json }";

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventEnvelopeReader.read(invalidMessage)
        );

        assertEquals("Cannot deserialize EventEnvelope", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldDeserializePayloadFromJsonNode() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        String payloadJson = """
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "activationToken": "token-123"
                }
                """.formatted(userId);

        JsonNode payloadNode = objectMapper.readTree(payloadJson);

        // when
        UserRegisteredV1Payload payload = eventEnvelopeReader.readPayload(payloadNode, UserRegisteredV1Payload.class);

        // then
        assertEquals(userId, payload.userId());
        assertEquals("user@example.com", payload.email());
        assertEquals("token-123", payload.activationToken());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenPayloadCannotBeDeserialized() {
        // given
        JsonNode payloadNode = objectMapper.createObjectNode().put("userId", "not-a-uuid");

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventEnvelopeReader.readPayload(payloadNode, UserRegisteredV1Payload.class)
        );

        assertEquals("Cannot deserialize payload to UserRegisteredV1Payload", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}