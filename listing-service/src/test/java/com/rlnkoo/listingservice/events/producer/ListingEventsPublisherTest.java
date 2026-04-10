package com.rlnkoo.listingservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.listingservice.events.types.ListingArchivedPayload;
import com.rlnkoo.listingservice.events.types.ListingPublishedPayload;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListingEventsPublisherTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PHOTO_ID_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTO_ID_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ListingEventsPublisher listingEventsPublisher;

    @Test
    void shouldPublishListingUpdatedEventToListingTopic() {
        // given
        UUID listingId = UUID.randomUUID();
        ListingUpdatedPayload payload = ListingUpdatedPayload.builder()
                .listingId(listingId)
                .ownerId(OWNER_ID)
                .status("PUBLISHED")
                .version(4)
                .title("Modern apartment")
                .description("Updated listing description")
                .priceAmount(new BigDecimal("799000.00"))
                .currencyCode("PLN")
                .address(ListingUpdatedPayload.AddressPayload.builder()
                        .country("England")
                        .city("London")
                        .street("Main Street 12")
                        .postalCode("00-100")
                        .build())
                .area(new BigDecimal("72.50"))
                .rooms(4)
                .floor(5)
                .propertyType("APARTMENT")
                .photoIds(List.of(PHOTO_ID_1, PHOTO_ID_2))
                .build();

        // when
        listingEventsPublisher.publishListingUpdated(listingId, payload);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<ListingUpdatedPayload>> envelopeCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(eventPublisher).publish(
                eq(Topics.LISTING_EVENTS),
                eq(listingId.toString()),
                envelopeCaptor.capture()
        );

        EventEnvelope<ListingUpdatedPayload> envelope = envelopeCaptor.getValue();
        assertNotNull(envelope);
        assertEquals("ListingUpdatedV1", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());
        assertSame(payload, envelope.payload());

        ListingUpdatedPayload capturedPayload = envelope.payload();
        assertEquals(listingId, capturedPayload.listingId());
        assertEquals(OWNER_ID, capturedPayload.ownerId());
        assertEquals("PUBLISHED", capturedPayload.status());
        assertEquals(4, capturedPayload.version());
        assertEquals("Modern apartment", capturedPayload.title());
        assertEquals("Updated listing description", capturedPayload.description());
        assertEquals(new BigDecimal("799000.00"), capturedPayload.priceAmount());
        assertEquals("PLN", capturedPayload.currencyCode());
        assertNotNull(capturedPayload.address());
        assertEquals("England", capturedPayload.address().country());
        assertEquals("London", capturedPayload.address().city());
        assertEquals("Main Street 12", capturedPayload.address().street());
        assertEquals("00-100", capturedPayload.address().postalCode());
        assertEquals(new BigDecimal("72.50"), capturedPayload.area());
        assertEquals(4, capturedPayload.rooms());
        assertEquals(5, capturedPayload.floor());
        assertEquals("APARTMENT", capturedPayload.propertyType());
        assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), capturedPayload.photoIds());
    }

    @Test
    void shouldPublishListingPublishedEventToListingTopic() {
        // given
        UUID listingId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-01-15T12:30:00Z");

        ListingPublishedPayload payload = ListingPublishedPayload.builder()
                .listingId(listingId)
                .ownerId(OWNER_ID)
                .status("PUBLISHED")
                .version(3)
                .publishedAt(publishedAt)
                .title("House with garden")
                .description("Beautiful detached house")
                .priceAmount(new BigDecimal("1250000.00"))
                .currencyCode("PLN")
                .address(ListingUpdatedPayload.AddressPayload.builder()
                        .country("England")
                        .city("Manchester")
                        .street("Garden Street 7")
                        .postalCode("30-200")
                        .build())
                .area(new BigDecimal("145.00"))
                .rooms(5)
                .floor(1)
                .propertyType("HOUSE")
                .photoIds(List.of(PHOTO_ID_1))
                .build();

        // when
        listingEventsPublisher.publishListingPublished(listingId, payload);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<ListingPublishedPayload>> envelopeCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(eventPublisher).publish(
                eq(Topics.LISTING_EVENTS),
                eq(listingId.toString()),
                envelopeCaptor.capture()
        );

        EventEnvelope<ListingPublishedPayload> envelope = envelopeCaptor.getValue();
        assertNotNull(envelope);
        assertEquals("ListingPublishedV1", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());
        assertSame(payload, envelope.payload());

        ListingPublishedPayload capturedPayload = envelope.payload();
        assertEquals(listingId, capturedPayload.listingId());
        assertEquals(OWNER_ID, capturedPayload.ownerId());
        assertEquals("PUBLISHED", capturedPayload.status());
        assertEquals(3, capturedPayload.version());
        assertEquals(publishedAt, capturedPayload.publishedAt());
        assertEquals("House with garden", capturedPayload.title());
        assertEquals("Beautiful detached house", capturedPayload.description());
        assertEquals(new BigDecimal("1250000.00"), capturedPayload.priceAmount());
        assertEquals("PLN", capturedPayload.currencyCode());
        assertNotNull(capturedPayload.address());
        assertEquals("England", capturedPayload.address().country());
        assertEquals("Manchester", capturedPayload.address().city());
        assertEquals("Garden Street 7", capturedPayload.address().street());
        assertEquals("30-200", capturedPayload.address().postalCode());
        assertEquals(new BigDecimal("145.00"), capturedPayload.area());
        assertEquals(5, capturedPayload.rooms());
        assertEquals(1, capturedPayload.floor());
        assertEquals("HOUSE", capturedPayload.propertyType());
        assertEquals(List.of(PHOTO_ID_1), capturedPayload.photoIds());
    }

    @Test
    void shouldPublishListingArchivedEventToListingTopic() {
        // given
        UUID listingId = UUID.randomUUID();
        Instant archivedAt = Instant.parse("2025-01-16T09:45:00Z");

        ListingArchivedPayload payload = ListingArchivedPayload.builder()
                .listingId(listingId)
                .ownerId(OWNER_ID)
                .status("ARCHIVED")
                .version(5)
                .archivedAt(archivedAt)
                .build();

        // when
        listingEventsPublisher.publishListingArchived(listingId, payload);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<ListingArchivedPayload>> envelopeCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(eventPublisher).publish(
                eq(Topics.LISTING_EVENTS),
                eq(listingId.toString()),
                envelopeCaptor.capture()
        );

        EventEnvelope<ListingArchivedPayload> envelope = envelopeCaptor.getValue();
        assertNotNull(envelope);
        assertEquals("ListingArchivedV1", envelope.eventType());
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());
        assertSame(payload, envelope.payload());

        ListingArchivedPayload capturedPayload = envelope.payload();
        assertEquals(listingId, capturedPayload.listingId());
        assertEquals(OWNER_ID, capturedPayload.ownerId());
        assertEquals("ARCHIVED", capturedPayload.status());
        assertEquals(5, capturedPayload.version());
        assertEquals(archivedAt, capturedPayload.archivedAt());
    }
}