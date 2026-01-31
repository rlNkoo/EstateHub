package com.rlnkoo.mediaservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.mediaservice.events.types.PhotoDeletedPayload;
import com.rlnkoo.mediaservice.events.types.PhotoUploadedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MediaEventsPublisher {

    private final EventPublisher eventPublisher;

    public void publishPhotoUploaded(UUID mediaId, PhotoUploadedPayload payload) {
        EventEnvelope<PhotoUploadedPayload> envelope =
                EventEnvelope.of("PhotoUploadedV1", payload);

        eventPublisher.publish(Topics.MEDIA_EVENTS, mediaId.toString(), envelope);
    }

    public void publishPhotoDeleted(UUID mediaId, PhotoDeletedPayload payload) {
        EventEnvelope<PhotoDeletedPayload> envelope =
                EventEnvelope.of("PhotoDeletedV1", payload);

        eventPublisher.publish(Topics.MEDIA_EVENTS, mediaId.toString(), envelope);
    }
}