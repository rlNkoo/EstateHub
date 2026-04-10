package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import com.rlnkoo.searchservice.integration.kafka.events.ListingArchivedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingEventAddressPayload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingPublishedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingUpdatedV1Payload;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingIndexingServiceTest {

    @Mock
    private SearchListingRepository searchListingRepository;

    @InjectMocks
    private ListingIndexingService listingIndexingService;

    @Test
    void shouldSaveDocumentWhenPublishedEventHasPublishedStatus() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-02-01T10:00:00Z");

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                3,
                publishedAt,
                "Modern apartment",
                "Bright apartment in city center",
                new BigDecimal("650000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "London", "Main Street", "00-001"),
                new BigDecimal("72.50"),
                3,
                4,
                "APARTMENT",
                List.of(photoId)
        );

        // when
        listingIndexingService.onListingPublished(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertEquals(listingId, document.getId());
        assertEquals(ownerId, document.getOwnerId());
        assertEquals("PUBLISHED", document.getStatus());
        assertEquals(3, document.getVersion());
        assertEquals(publishedAt, document.getPublishedAt());
        assertEquals("Modern apartment", document.getTitle());
        assertEquals("Bright apartment in city center", document.getDescription());
        assertEquals(new BigDecimal("650000.00"), document.getPriceAmount());
        assertEquals("PLN", document.getCurrencyCode());
        assertEquals("England", document.getCountry());
        assertEquals("London", document.getCity());
        assertEquals("Main Street", document.getStreet());
        assertEquals("00-001", document.getPostalCode());
        assertEquals(new BigDecimal("72.50"), document.getArea());
        assertEquals(3, document.getRooms());
        assertEquals(4, document.getFloor());
        assertEquals("APARTMENT", document.getPropertyType());
        assertEquals(List.of(photoId), document.getPhotoIds());
        assertNotNull(document.getIndexedAt());
    }

    @Test
    void shouldSkipPublishedEventWhenStatusIsNotPublished() {
        // given
        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DRAFT",
                1,
                Instant.parse("2025-02-01T10:00:00Z"),
                "Draft apartment",
                "Draft description",
                new BigDecimal("500000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "London", "Main Street", "00-001"),
                new BigDecimal("60.00"),
                2,
                1,
                "APARTMENT",
                List.of(UUID.randomUUID())
        );

        // when
        listingIndexingService.onListingPublished(payload);

        // then
        verify(searchListingRepository, never()).save(any());
    }

    @Test
    void shouldMapNullAddressToNullFieldsWhenPublished() {
        // given
        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PUBLISHED",
                1,
                Instant.parse("2025-02-01T10:00:00Z"),
                "Apartment without address",
                "Description",
                new BigDecimal("400000.00"),
                "PLN",
                null,
                new BigDecimal("45.00"),
                2,
                1,
                "APARTMENT",
                List.of(UUID.randomUUID())
        );

        // when
        listingIndexingService.onListingPublished(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertNull(document.getCountry());
        assertNull(document.getCity());
        assertNull(document.getStreet());
        assertNull(document.getPostalCode());
    }

    @Test
    void shouldMapNullPhotoIdsToEmptyListWhenPublished() {
        // given
        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PUBLISHED",
                1,
                Instant.parse("2025-02-01T10:00:00Z"),
                "Apartment without photos",
                "Description",
                new BigDecimal("400000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "London", "Main Street", "00-001"),
                new BigDecimal("45.00"),
                2,
                1,
                "APARTMENT",
                null
        );

        // when
        listingIndexingService.onListingPublished(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertNotNull(document.getPhotoIds());
        assertTrue(document.getPhotoIds().isEmpty());
    }

    @Test
    void shouldUpdateExistingDocumentAndPreservePublishedAt() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-01-01T09:00:00Z");
        UUID newPhotoId = UUID.randomUUID();

        SearchListingDocument existing = SearchListingDocument.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status("PUBLISHED")
                .version(1)
                .publishedAt(publishedAt)
                .title("Old title")
                .build();

        when(searchListingRepository.findById(listingId)).thenReturn(Optional.of(existing));

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment",
                "Updated description",
                new BigDecimal("720000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "Manchester", "River Street", "80-001"),
                new BigDecimal("80.00"),
                4,
                6,
                "APARTMENT",
                List.of(newPhotoId)
        );

        // when
        listingIndexingService.onListingUpdated(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertEquals(listingId, document.getId());
        assertEquals(ownerId, document.getOwnerId());
        assertEquals("PUBLISHED", document.getStatus());
        assertEquals(2, document.getVersion());
        assertEquals(publishedAt, document.getPublishedAt());
        assertEquals("Updated apartment", document.getTitle());
        assertEquals("Updated description", document.getDescription());
        assertEquals(new BigDecimal("720000.00"), document.getPriceAmount());
        assertEquals("PLN", document.getCurrencyCode());
        assertEquals("England", document.getCountry());
        assertEquals("Manchester", document.getCity());
        assertEquals("River Street", document.getStreet());
        assertEquals("80-001", document.getPostalCode());
        assertEquals(new BigDecimal("80.00"), document.getArea());
        assertEquals(4, document.getRooms());
        assertEquals(6, document.getFloor());
        assertEquals("APARTMENT", document.getPropertyType());
        assertEquals(List.of(newPhotoId), document.getPhotoIds());
        assertNotNull(document.getIndexedAt());
    }

    @Test
    void shouldCreateNewDocumentWhenUpdatedListingDoesNotExist() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(searchListingRepository.findById(listingId)).thenReturn(Optional.empty());

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                "New apartment",
                "New description",
                new BigDecimal("580000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "Bristol", "Lake Street", "50-001"),
                new BigDecimal("63.00"),
                3,
                2,
                "APARTMENT",
                List.of(UUID.randomUUID())
        );

        // when
        listingIndexingService.onListingUpdated(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertEquals(listingId, document.getId());
        assertEquals(ownerId, document.getOwnerId());
        assertNull(document.getPublishedAt());
        assertEquals("New apartment", document.getTitle());
    }

    @Test
    void shouldSkipUpdatedEventWhenStatusIsNotPublished() {
        // given
        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ARCHIVED",
                5,
                "Archived apartment",
                "Description",
                new BigDecimal("300000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "London", "Main Street", "00-001"),
                new BigDecimal("40.00"),
                2,
                0,
                "APARTMENT",
                List.of(UUID.randomUUID())
        );

        // when
        listingIndexingService.onListingUpdated(payload);

        // then
        verify(searchListingRepository, never()).findById(any());
        verify(searchListingRepository, never()).save(any());
    }

    @Test
    void shouldMapNullPhotoIdsToEmptyListWhenUpdated() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        when(searchListingRepository.findById(listingId)).thenReturn(Optional.empty());

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment",
                "Updated description",
                new BigDecimal("600000.00"),
                "PLN",
                new ListingEventAddressPayload("England", "London", "Main Street", "00-001"),
                new BigDecimal("55.00"),
                3,
                2,
                "APARTMENT",
                null
        );

        // when
        listingIndexingService.onListingUpdated(payload);

        // then
        ArgumentCaptor<SearchListingDocument> captor = ArgumentCaptor.forClass(SearchListingDocument.class);
        verify(searchListingRepository).save(captor.capture());

        SearchListingDocument document = captor.getValue();
        assertNotNull(document.getPhotoIds());
        assertTrue(document.getPhotoIds().isEmpty());
    }

    @Test
    void shouldDeleteDocumentWhenArchivedListingExists() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                UUID.randomUUID(),
                "ARCHIVED",
                4,
                Instant.parse("2025-02-10T10:00:00Z")
        );

        when(searchListingRepository.existsById(listingId)).thenReturn(true);

        // when
        listingIndexingService.onListingArchived(payload);

        // then
        verify(searchListingRepository).existsById(listingId);
        verify(searchListingRepository).deleteById(listingId);
    }

    @Test
    void shouldDoNothingWhenArchivedListingDoesNotExist() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                UUID.randomUUID(),
                "ARCHIVED",
                4,
                Instant.parse("2025-02-10T10:00:00Z")
        );

        when(searchListingRepository.existsById(listingId)).thenReturn(false);

        // when
        listingIndexingService.onListingArchived(payload);

        // then
        verify(searchListingRepository).existsById(listingId);
        verify(searchListingRepository, never()).deleteById(any());
    }

    @Test
    void shouldDoNothingWhenArchivedListingIdIsNull() {
        // given
        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                null,
                UUID.randomUUID(),
                "ARCHIVED",
                4,
                Instant.parse("2025-02-10T10:00:00Z")
        );

        // when
        listingIndexingService.onListingArchived(payload);

        // then
        verify(searchListingRepository, never()).existsById(any());
        verify(searchListingRepository, never()).deleteById(any());
    }
}