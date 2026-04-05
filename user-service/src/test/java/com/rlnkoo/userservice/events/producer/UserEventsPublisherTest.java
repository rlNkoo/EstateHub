package com.rlnkoo.userservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class UserEventsPublisherTest {

    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final UserEventsPublisher userEventsPublisher =
            new UserEventsPublisher(eventPublisher);

    @Test
    void shouldPublishUserEvent() {
        // given
        UUID userId = UUID.randomUUID();
        EventEnvelope<String> envelope = EventEnvelope.of("TestEvent", "payload");

        // when
        userEventsPublisher.publish(userId, envelope);

        // then
        verify(eventPublisher).publish(
                Topics.USER_EVENTS,
                userId.toString(),
                envelope
        );
    }
}