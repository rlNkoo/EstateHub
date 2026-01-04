package com.rlnkoo.userservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserEventsPublisher {

    private final EventPublisher eventPublisher;

    public void publish(UUID userId, EventEnvelope<?> envelope) {
        eventPublisher.publish(Topics.USER_EVENTS, userId.toString(), envelope);
    }
}