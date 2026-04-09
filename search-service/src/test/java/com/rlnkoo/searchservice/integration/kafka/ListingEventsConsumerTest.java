package com.rlnkoo.searchservice.integration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.searchservice.domain.service.ListingIndexingService;
import com.rlnkoo.searchservice.integration.kafka.events.ListingArchivedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingPublishedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingUpdatedV1Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingEventsConsumerTest {

    private ObjectMapper objectMapper;
    private ListingEventsConsumer listingEventsConsumer;

    @Mock
    private ListingIndexingService indexingService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        listingEventsConsumer = new ListingEventsConsumer(objectMapper, indexingService);
    }

    @Test
    void shouldHandleListingPublishedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-02-01T10:00:00Z");

        String message = """
                {
                  "eventType": "ListingPublishedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": "%s",
                    "status": "PUBLISHED",
                    "version": 3,
                    "publishedAt": "%s",
                    "title": "Modern apartment",
                    "description": "Bright apartment in city center",
                    "priceAmount": 650000.00,
                    "currencyCode": "PLN",
                    "address": {
                      "country": "Poland",
                      "city": "Warsaw",
                      "street": "Main Street",
                      "postalCode": "00-001"
                    },
                    "area": 72.50,
                    "rooms": 3,
                    "floor": 4,
                    "propertyType": "APARTMENT",
                    "photoIds": ["%s"]
                  }
                }
                """.formatted(listingId, ownerId, publishedAt, photoId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        ArgumentCaptor<ListingPublishedV1Payload> captor =
                ArgumentCaptor.forClass(ListingPublishedV1Payload.class);

        verify(indexingService).onListingPublished(captor.capture());

        ListingPublishedV1Payload payload = captor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(ownerId, payload.ownerId());
        assertEquals("PUBLISHED", payload.status());
        assertEquals(3, payload.version());
        assertEquals(publishedAt, payload.publishedAt());
        assertEquals("Modern apartment", payload.title());
        assertEquals("Bright apartment in city center", payload.description());
        assertEquals(0, new BigDecimal("650000.00").compareTo(payload.priceAmount()));
        assertEquals("PLN", payload.currencyCode());
        assertNotNull(payload.address());
        assertEquals("Poland", payload.address().country());
        assertEquals("Warsaw", payload.address().city());
        assertEquals("Main Street", payload.address().street());
        assertEquals("00-001", payload.address().postalCode());
        assertEquals(0, new BigDecimal("72.50").compareTo(payload.area()));
        assertEquals(3, payload.rooms());
        assertEquals(4, payload.floor());
        assertEquals("APARTMENT", payload.propertyType());
        assertEquals(List.of(photoId), payload.photoIds());

        verify(indexingService, never()).onListingUpdated(any());
        verify(indexingService, never()).onListingArchived(any());
    }

    @Test
    void shouldHandleListingUpdatedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingUpdatedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": "%s",
                    "status": "PUBLISHED",
                    "version": 4,
                    "title": "Updated apartment",
                    "description": "Updated description",
                    "priceAmount": 720000.00,
                    "currencyCode": "PLN",
                    "address": {
                      "country": "Poland",
                      "city": "Gdansk",
                      "street": "River Street",
                      "postalCode": "80-001"
                    },
                    "area": 80.00,
                    "rooms": 4,
                    "floor": 6,
                    "propertyType": "APARTMENT",
                    "photoIds": ["%s"]
                  }
                }
                """.formatted(listingId, ownerId, photoId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        ArgumentCaptor<ListingUpdatedV1Payload> captor =
                ArgumentCaptor.forClass(ListingUpdatedV1Payload.class);

        verify(indexingService).onListingUpdated(captor.capture());

        ListingUpdatedV1Payload payload = captor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(ownerId, payload.ownerId());
        assertEquals("PUBLISHED", payload.status());
        assertEquals(4, payload.version());
        assertEquals("Updated apartment", payload.title());
        assertEquals("Updated description", payload.description());
        assertEquals(0, new BigDecimal("720000.00").compareTo(payload.priceAmount()));
        assertEquals("PLN", payload.currencyCode());
        assertNotNull(payload.address());
        assertEquals("Poland", payload.address().country());
        assertEquals("Gdansk", payload.address().city());
        assertEquals("River Street", payload.address().street());
        assertEquals("80-001", payload.address().postalCode());
        assertEquals(0, new BigDecimal("80.00").compareTo(payload.area()));
        assertEquals(4, payload.rooms());
        assertEquals(6, payload.floor());
        assertEquals("APARTMENT", payload.propertyType());
        assertEquals(List.of(photoId), payload.photoIds());

        verify(indexingService, never()).onListingPublished(any());
        verify(indexingService, never()).onListingArchived(any());
    }

    @Test
    void shouldHandleListingArchivedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant archivedAt = Instant.parse("2025-02-10T10:00:00Z");

        String message = """
                {
                  "eventType": "ListingArchivedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": "%s",
                    "status": "ARCHIVED",
                    "version": 5,
                    "archivedAt": "%s"
                  }
                }
                """.formatted(listingId, ownerId, archivedAt);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        ArgumentCaptor<ListingArchivedV1Payload> captor =
                ArgumentCaptor.forClass(ListingArchivedV1Payload.class);

        verify(indexingService).onListingArchived(captor.capture());

        ListingArchivedV1Payload payload = captor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(ownerId, payload.ownerId());
        assertEquals("ARCHIVED", payload.status());
        assertEquals(5, payload.version());
        assertEquals(archivedAt, payload.archivedAt());

        verify(indexingService, never()).onListingPublished(any());
        verify(indexingService, never()).onListingUpdated(any());
    }

    @Test
    void shouldSkipEventWhenEventTypeIsMissing() {
        // given
        String message = """
                {
                  "payload": {
                    "listingId": "%s",
                    "ownerId": "%s"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldIgnoreUnsupportedEventType() {
        // given
        String message = """
                {
                  "eventType": "ListingDeletedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": "%s"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipPublishedEventWhenListingIdIsMissing() {
        // given
        UUID ownerId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingPublishedV1",
                  "payload": {
                    "listingId": null,
                    "ownerId": "%s",
                    "status": "PUBLISHED",
                    "version": 1
                  }
                }
                """.formatted(ownerId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipPublishedEventWhenOwnerIdIsMissing() {
        // given
        UUID listingId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingPublishedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": null,
                    "status": "PUBLISHED",
                    "version": 1
                  }
                }
                """.formatted(listingId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipUpdatedEventWhenListingIdIsMissing() {
        // given
        UUID ownerId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingUpdatedV1",
                  "payload": {
                    "listingId": null,
                    "ownerId": "%s",
                    "status": "PUBLISHED",
                    "version": 1
                  }
                }
                """.formatted(ownerId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipUpdatedEventWhenOwnerIdIsMissing() {
        // given
        UUID listingId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingUpdatedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": null,
                    "status": "PUBLISHED",
                    "version": 1
                  }
                }
                """.formatted(listingId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipArchivedEventWhenListingIdIsMissing() {
        // given
        UUID ownerId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingArchivedV1",
                  "payload": {
                    "listingId": null,
                    "ownerId": "%s",
                    "status": "ARCHIVED",
                    "version": 1,
                    "archivedAt": "2025-02-10T10:00:00Z"
                  }
                }
                """.formatted(ownerId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldSkipArchivedEventWhenOwnerIdIsMissing() {
        // given
        UUID listingId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "ListingArchivedV1",
                  "payload": {
                    "listingId": "%s",
                    "ownerId": null,
                    "status": "ARCHIVED",
                    "version": 1,
                    "archivedAt": "2025-02-10T10:00:00Z"
                  }
                }
                """.formatted(listingId);

        // when
        listingEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(indexingService);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenMessageCannotBeParsed() {
        // given
        String invalidMessage = "{ invalid-json }";

        // when + then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> listingEventsConsumer.onMessage(invalidMessage)
        );

        assertNotNull(exception.getCause());
        verifyNoInteractions(indexingService);
    }
}