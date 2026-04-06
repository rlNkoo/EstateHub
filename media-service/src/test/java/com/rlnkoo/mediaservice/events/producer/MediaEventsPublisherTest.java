package com.rlnkoo.mediaservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.mediaservice.events.types.PhotoDeletedPayload;
import com.rlnkoo.mediaservice.events.types.PhotoUploadedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaEventsPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private MediaEventsPublisher mediaEventsPublisher;

    @Test
    void shouldPublishPhotoUploadedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant uploadedAt = Instant.now();

        PhotoUploadedPayload payload = PhotoUploadedPayload.builder()
                .mediaId(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .bucket("estatehub-media")
                .objectKey("listings/" + listingId + "/photos/" + mediaId + ".jpg")
                .thumbnailKey("listings/" + listingId + "/thumbs/" + mediaId + ".jpg")
                .contentType("image/jpeg")
                .sizeBytes(12345L)
                .sha256("sha256-hash")
                .uploadedAt(uploadedAt)
                .build();

        // when
        mediaEventsPublisher.publishPhotoUploaded(listingId, payload);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<PhotoUploadedPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(eventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(Topics.MEDIA_EVENTS),
                org.mockito.ArgumentMatchers.eq(listingId.toString()),
                eventCaptor.capture()
        );

        EventEnvelope<PhotoUploadedPayload> envelope = eventCaptor.getValue();
        assertEquals("PhotoUploadedV1", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());

        PhotoUploadedPayload capturedPayload = envelope.payload();
        assertEquals(mediaId, capturedPayload.mediaId());
        assertEquals(listingId, capturedPayload.listingId());
        assertEquals(ownerId, capturedPayload.ownerId());
        assertEquals("estatehub-media", capturedPayload.bucket());
        assertEquals("listings/" + listingId + "/photos/" + mediaId + ".jpg", capturedPayload.objectKey());
        assertEquals("listings/" + listingId + "/thumbs/" + mediaId + ".jpg", capturedPayload.thumbnailKey());
        assertEquals("image/jpeg", capturedPayload.contentType());
        assertEquals(12345L, capturedPayload.sizeBytes());
        assertEquals("sha256-hash", capturedPayload.sha256());
        assertEquals(uploadedAt, capturedPayload.uploadedAt());
    }

    @Test
    void shouldPublishPhotoDeletedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant deletedAt = Instant.now();

        PhotoDeletedPayload payload = PhotoDeletedPayload.builder()
                .mediaId(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .deletedAt(deletedAt)
                .build();

        // when
        mediaEventsPublisher.publishPhotoDeleted(listingId, payload);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<PhotoDeletedPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(eventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(Topics.MEDIA_EVENTS),
                org.mockito.ArgumentMatchers.eq(listingId.toString()),
                eventCaptor.capture()
        );

        EventEnvelope<PhotoDeletedPayload> envelope = eventCaptor.getValue();
        assertEquals("PhotoDeletedV1", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());

        PhotoDeletedPayload capturedPayload = envelope.payload();
        assertEquals(mediaId, capturedPayload.mediaId());
        assertEquals(listingId, capturedPayload.listingId());
        assertEquals(ownerId, capturedPayload.ownerId());
        assertEquals(deletedAt, capturedPayload.deletedAt());
    }
}