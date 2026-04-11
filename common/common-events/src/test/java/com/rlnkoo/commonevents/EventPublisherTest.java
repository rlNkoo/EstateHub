package com.rlnkoo.commonevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventPublisherTest {

    @Test
    void shouldPublishSerializedEventToKafka() {
        // given
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        EventPublisher publisher = new EventPublisher(kafkaTemplate, objectMapper);
        EventEnvelope<String> event = EventEnvelope.of("TEST", "payload");

        // when
        publisher.publish("topic", "key", event);

        // then
        verify(kafkaTemplate, times(1))
                .send(eq("topic"), eq("key"), anyString());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSerializationFails() throws Exception {
        // given
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        EventPublisher publisher = new EventPublisher(kafkaTemplate, objectMapper);

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("error"));

        // when + then
        assertThrows(IllegalStateException.class,
                () -> publisher.publish("topic", "key", new Object()));
    }
}