package com.rlnkoo.commonevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, String key, Object eventEnvelope) {
        try {
            String json = objectMapper.writeValueAsString(eventEnvelope);
            kafkaTemplate.send(topic, key, json);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize event to JSON", e);
        }
    }
}
