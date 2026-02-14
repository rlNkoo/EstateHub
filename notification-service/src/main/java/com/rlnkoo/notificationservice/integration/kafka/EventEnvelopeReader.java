package com.rlnkoo.notificationservice.integration.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.commonevents.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEnvelopeReader {

    private static final TypeReference<EventEnvelope<JsonNode>> ENVELOPE_JSON =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public EventEnvelope<JsonNode> read(String message) {
        try {
            return objectMapper.readValue(message, ENVELOPE_JSON);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize EventEnvelope", e);
        }
    }

    public <T> T readPayload(JsonNode payloadNode, Class<T> payloadClass) {
        try {
            return objectMapper.treeToValue(payloadNode, payloadClass);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize payload to " + payloadClass.getSimpleName(), e);
        }
    }
}