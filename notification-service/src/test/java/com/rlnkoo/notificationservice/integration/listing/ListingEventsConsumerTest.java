package com.rlnkoo.notificationservice.integration.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.domain.service.ListingNotificationService;
import com.rlnkoo.notificationservice.integration.kafka.EventEnvelopeReader;
import com.rlnkoo.notificationservice.integration.listing.events.ListingArchivedV1Payload;
import com.rlnkoo.notificationservice.integration.listing.events.ListingPublishedV1Payload;
import com.rlnkoo.notificationservice.integration.listing.events.ListingUpdatedV1Payload;
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
class ListingEventsConsumerTest {

    @Mock
    private EventEnvelopeReader reader;

    @Mock
    private ListingNotificationService listingNotificationService;

    @InjectMocks
    private ListingEventsConsumer listingEventsConsumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldHandleListingPublishedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant publishedAt = Instant.now();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "ownerId": "%s",
                  "status": "PUBLISHED",
                  "version": 1,
                  "publishedAt": "%s",
                  "title": "Beautiful apartment"
                }
                """.formatted(listingId, ownerId, publishedAt));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                payloadNode
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                publishedAt,
                "Beautiful apartment"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingPublishedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingPublishedV1Payload.class);
        verify(listingNotificationService).onListingPublished(envelope, payload);
        verifyNoMoreInteractions(listingNotificationService);
    }

    @Test
    void shouldIgnoreListingPublishedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "ownerId": "%s",
                  "status": "PUBLISHED",
                  "version": 1,
                  "title": "Beautiful apartment"
                }
                """.formatted(listingId, ownerId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                payloadNode
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                null,
                "Beautiful apartment"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingPublishedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingPublishedV1Payload.class);
        verifyNoInteractions(listingNotificationService);
    }

    @Test
    void shouldHandleListingUpdatedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "ownerId": "%s",
                  "status": "PUBLISHED",
                  "version": 2,
                  "title": "Updated apartment"
                }
                """.formatted(listingId, ownerId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                payloadNode
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingUpdatedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingUpdatedV1Payload.class);
        verify(listingNotificationService).onListingUpdated(envelope, payload);
        verifyNoMoreInteractions(listingNotificationService);
    }

    @Test
    void shouldIgnoreListingUpdatedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "status": "PUBLISHED",
                  "version": 2,
                  "title": "Updated apartment"
                }
                """.formatted(listingId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                payloadNode
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                null,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingUpdatedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingUpdatedV1Payload.class);
        verifyNoInteractions(listingNotificationService);
    }

    @Test
    void shouldHandleListingArchivedEvent() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant archivedAt = Instant.now();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "ownerId": "%s",
                  "status": "ARCHIVED",
                  "version": 3,
                  "archivedAt": "%s"
                }
                """.formatted(listingId, ownerId, archivedAt));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                payloadNode
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                archivedAt
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingArchivedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingArchivedV1Payload.class);
        verify(listingNotificationService).onListingArchived(envelope, payload);
        verifyNoMoreInteractions(listingNotificationService);
    }

    @Test
    void shouldIgnoreListingArchivedEventWhenPayloadIsMissingRequiredFields() throws Exception {
        // given
        String message = "message";
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JsonNode payloadNode = objectMapper.readTree("""
                {
                  "listingId": "%s",
                  "ownerId": "%s",
                  "status": "ARCHIVED",
                  "version": 3
                }
                """.formatted(listingId, ownerId));

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                payloadNode
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                null
        );

        when(reader.read(message)).thenReturn(envelope);
        when(reader.readPayload(payloadNode, ListingArchivedV1Payload.class)).thenReturn(payload);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader).readPayload(payloadNode, ListingArchivedV1Payload.class);
        verifyNoInteractions(listingNotificationService);
    }

    @Test
    void shouldIgnoreListingEventWhenEnvelopeIsMissingRequiredFields() {
        // given
        String message = "message";

        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
                null,
                "ListingPublishedV1",
                Instant.now(),
                objectMapper.createObjectNode()
        );

        when(reader.read(message)).thenReturn(envelope);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader, never()).readPayload(any(), any());
        verifyNoInteractions(listingNotificationService);
    }

    @Test
    void shouldIgnoreUnknownListingEventType() {
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
        listingEventsConsumer.onMessage(message);

        // then
        verify(reader).read(message);
        verify(reader, never()).readPayload(any(), any());
        verifyNoInteractions(listingNotificationService);
    }

    @Test
    void shouldRethrowExceptionWhenReaderFailsToDeserializeListingEvent() {
        // given
        String message = "message";
        RuntimeException exception = new RuntimeException("cannot deserialize");

        when(reader.read(message)).thenThrow(exception);

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listingEventsConsumer.onMessage(message)
        );

        assertSame(exception, thrown);
        verify(reader).read(message);
        verifyNoInteractions(listingNotificationService);
    }
}