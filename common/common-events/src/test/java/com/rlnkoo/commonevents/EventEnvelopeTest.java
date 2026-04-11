package com.rlnkoo.commonevents;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventEnvelopeTest {

    @Test
    void shouldCreateEventEnvelopeWithGeneratedValues() {
        String eventType = "USER_CREATED";
        String payload = "data";

        EventEnvelope<String> envelope = EventEnvelope.of(eventType, payload);

        assertNotNull(envelope.eventId());
        assertEquals(eventType, envelope.eventType());
        assertNotNull(envelope.occurredAt());
        assertEquals(payload, envelope.payload());
    }
}