package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.api.admin.dto.ReindexResponse;
import com.rlnkoo.searchservice.config.ReindexProperties;
import com.rlnkoo.searchservice.domain.exception.ReindexFailedException;
import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import com.rlnkoo.searchservice.integration.listing.ListingServiceClient;
import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingForReindexResponse;
import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingsPageResponse;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import com.rlnkoo.searchservice.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingReindexServiceTest {

    @Mock
    private ListingServiceClient listingServiceClient;

    @Mock
    private SearchListingRepository searchListingRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ReindexProperties reindexProperties;

    @InjectMocks
    private ListingReindexService listingReindexService;

    @Test
    void shouldReindexSinglePageSuccessfully() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2025-02-01T10:00:00Z");

        PublishedListingForReindexResponse item = PublishedListingForReindexResponse.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status("PUBLISHED")
                .version(3)
                .publishedAt(publishedAt)
                .updatedAt(Instant.parse("2025-02-02T10:00:00Z"))
                .title("Modern apartment")
                .description("Bright apartment in city center")
                .priceAmount(new BigDecimal("650000.00"))
                .currencyCode("PLN")
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country("Poland")
                        .city("Warsaw")
                        .street("Main Street")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("72.50"))
                .rooms(3)
                .floor(4)
                .propertyType("APARTMENT")
                .photoIds(List.of(photoId))
                .build();

        PublishedListingsPageResponse response = PublishedListingsPageResponse.builder()
                .items(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(100)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken)).thenReturn(response);

        // when
        ReindexResponse result = listingReindexService.reindexAllPublishedListings();

        // then
        assertNotNull(result);
        assertTrue(result.completed());
        assertEquals("Reindex completed successfully", result.message());
        assertEquals(1, result.fetchedCount());
        assertEquals(1, result.indexedCount());
        assertEquals(0, result.failedCount());
        assertEquals(1, result.processedPages());
        assertNotNull(result.timestamp());

        ArgumentCaptor<List<SearchListingDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(searchListingRepository).saveAll(captor.capture());

        List<SearchListingDocument> savedDocuments = captor.getValue();
        assertEquals(1, savedDocuments.size());

        SearchListingDocument document = savedDocuments.getFirst();
        assertEquals(listingId, document.getId());
        assertEquals(ownerId, document.getOwnerId());
        assertEquals("PUBLISHED", document.getStatus());
        assertEquals(3, document.getVersion());
        assertEquals(publishedAt, document.getPublishedAt());
        assertEquals("Modern apartment", document.getTitle());
        assertEquals("Bright apartment in city center", document.getDescription());
        assertEquals(new BigDecimal("650000.00"), document.getPriceAmount());
        assertEquals("PLN", document.getCurrencyCode());
        assertEquals("Poland", document.getCountry());
        assertEquals("Warsaw", document.getCity());
        assertEquals("Main Street", document.getStreet());
        assertEquals("00-001", document.getPostalCode());
        assertEquals(new BigDecimal("72.50"), document.getArea());
        assertEquals(3, document.getRooms());
        assertEquals(4, document.getFloor());
        assertEquals("APARTMENT", document.getPropertyType());
        assertEquals(List.of(photoId), document.getPhotoIds());
        assertNotNull(document.getIndexedAt());

        verify(currentUserProvider).requireCurrentJwt();
        verify(reindexProperties).getPageSize();
        verify(listingServiceClient).getPublishedListingsForReindex(0, 100, bearerToken);
        verify(searchListingRepository).saveAll(anyList());
    }

    @Test
    void shouldReindexMultiplePagesSuccessfully() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingForReindexResponse firstItem = PublishedListingForReindexResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status("PUBLISHED")
                .version(1)
                .publishedAt(Instant.parse("2025-02-01T10:00:00Z"))
                .title("Apartment A")
                .description("Description A")
                .priceAmount(new BigDecimal("500000.00"))
                .currencyCode("PLN")
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country("Poland")
                        .city("Warsaw")
                        .street("Street A")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("55.00"))
                .rooms(3)
                .floor(2)
                .propertyType("APARTMENT")
                .photoIds(List.of(UUID.randomUUID()))
                .build();

        PublishedListingForReindexResponse secondItem = PublishedListingForReindexResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status("PUBLISHED")
                .version(2)
                .publishedAt(Instant.parse("2025-02-03T10:00:00Z"))
                .title("House B")
                .description("Description B")
                .priceAmount(new BigDecimal("1200000.00"))
                .currencyCode("PLN")
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country("Poland")
                        .city("Krakow")
                        .street("Street B")
                        .postalCode("30-001")
                        .build())
                .area(new BigDecimal("140.00"))
                .rooms(5)
                .floor(0)
                .propertyType("HOUSE")
                .photoIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .build();

        PublishedListingsPageResponse firstPage = PublishedListingsPageResponse.builder()
                .items(List.of(firstItem))
                .totalElements(2)
                .totalPages(2)
                .page(0)
                .size(1)
                .build();

        PublishedListingsPageResponse secondPage = PublishedListingsPageResponse.builder()
                .items(List.of(secondItem))
                .totalElements(2)
                .totalPages(2)
                .page(1)
                .size(1)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(1);
        when(listingServiceClient.getPublishedListingsForReindex(0, 1, bearerToken)).thenReturn(firstPage);
        when(listingServiceClient.getPublishedListingsForReindex(1, 1, bearerToken)).thenReturn(secondPage);

        // when
        ReindexResponse result = listingReindexService.reindexAllPublishedListings();

        // then
        assertNotNull(result);
        assertTrue(result.completed());
        assertEquals(2, result.fetchedCount());
        assertEquals(2, result.indexedCount());
        assertEquals(0, result.failedCount());
        assertEquals(2, result.processedPages());

        verify(listingServiceClient).getPublishedListingsForReindex(0, 1, bearerToken);
        verify(listingServiceClient).getPublishedListingsForReindex(1, 1, bearerToken);
        verify(searchListingRepository, times(2)).saveAll(anyList());
    }

    @Test
    void shouldStopWhenListingServiceReturnsEmptyItems() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingsPageResponse emptyPage = PublishedListingsPageResponse.builder()
                .items(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(100)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken)).thenReturn(emptyPage);

        // when
        ReindexResponse result = listingReindexService.reindexAllPublishedListings();

        // then
        assertNotNull(result);
        assertTrue(result.completed());
        assertEquals(0, result.fetchedCount());
        assertEquals(0, result.indexedCount());
        assertEquals(0, result.failedCount());
        assertEquals(0, result.processedPages());

        verify(searchListingRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldMapNullAddressToNullFields() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingForReindexResponse item = PublishedListingForReindexResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status("PUBLISHED")
                .version(1)
                .publishedAt(Instant.parse("2025-02-01T10:00:00Z"))
                .title("Apartment without address")
                .description("Description")
                .priceAmount(new BigDecimal("350000.00"))
                .currencyCode("PLN")
                .address(null)
                .area(new BigDecimal("40.00"))
                .rooms(2)
                .floor(1)
                .propertyType("APARTMENT")
                .photoIds(List.of(UUID.randomUUID()))
                .build();

        PublishedListingsPageResponse response = PublishedListingsPageResponse.builder()
                .items(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(100)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken)).thenReturn(response);

        // when
        listingReindexService.reindexAllPublishedListings();

        // then
        ArgumentCaptor<List<SearchListingDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(searchListingRepository).saveAll(captor.capture());

        SearchListingDocument document = captor.getValue().getFirst();
        assertNull(document.getCountry());
        assertNull(document.getCity());
        assertNull(document.getStreet());
        assertNull(document.getPostalCode());
    }

    @Test
    void shouldMapNullPhotoIdsToEmptyList() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingForReindexResponse item = PublishedListingForReindexResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status("PUBLISHED")
                .version(1)
                .publishedAt(Instant.parse("2025-02-01T10:00:00Z"))
                .title("Apartment without photos")
                .description("Description")
                .priceAmount(new BigDecimal("350000.00"))
                .currencyCode("PLN")
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country("Poland")
                        .city("Warsaw")
                        .street("Main Street")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("40.00"))
                .rooms(2)
                .floor(1)
                .propertyType("APARTMENT")
                .photoIds(null)
                .build();

        PublishedListingsPageResponse response = PublishedListingsPageResponse.builder()
                .items(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(100)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken)).thenReturn(response);

        // when
        listingReindexService.reindexAllPublishedListings();

        // then
        ArgumentCaptor<List<SearchListingDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(searchListingRepository).saveAll(captor.capture());

        SearchListingDocument document = captor.getValue().getFirst();
        assertNotNull(document.getPhotoIds());
        assertTrue(document.getPhotoIds().isEmpty());
    }

    @Test
    void shouldUseBearerTokenFromCurrentUserJwt() {
        // given
        String bearerToken = "my-access-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingsPageResponse emptyPage = PublishedListingsPageResponse.builder()
                .items(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(50)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(50);
        when(listingServiceClient.getPublishedListingsForReindex(0, 50, bearerToken)).thenReturn(emptyPage);

        // when
        listingReindexService.reindexAllPublishedListings();

        // then
        verify(listingServiceClient).getPublishedListingsForReindex(0, 50, bearerToken);
    }

    @Test
    void shouldUseConfiguredPageSize() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingsPageResponse emptyPage = PublishedListingsPageResponse.builder()
                .items(List.of())
                .totalElements(0)
                .totalPages(0)
                .page(0)
                .size(25)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(25);
        when(listingServiceClient.getPublishedListingsForReindex(0, 25, bearerToken)).thenReturn(emptyPage);

        // when
        listingReindexService.reindexAllPublishedListings();

        // then
        verify(reindexProperties).getPageSize();
        verify(listingServiceClient).getPublishedListingsForReindex(0, 25, bearerToken);
    }

    @Test
    void shouldThrowReindexFailedExceptionWhenListingServiceClientFails() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken))
                .thenThrow(new RuntimeException("listing-service failure"));

        // when + then
        ReindexFailedException exception = assertThrows(
                ReindexFailedException.class,
                () -> listingReindexService.reindexAllPublishedListings()
        );

        assertEquals("Failed to reindex published listings", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("listing-service failure", exception.getCause().getMessage());

        verify(searchListingRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldThrowReindexFailedExceptionWhenRepositorySaveAllFails() {
        // given
        String bearerToken = "test-jwt-token";
        Jwt jwt = Jwt.withTokenValue(bearerToken)
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        PublishedListingForReindexResponse item = PublishedListingForReindexResponse.builder()
                .id(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .status("PUBLISHED")
                .version(1)
                .publishedAt(Instant.parse("2025-02-01T10:00:00Z"))
                .title("Apartment")
                .description("Description")
                .priceAmount(new BigDecimal("450000.00"))
                .currencyCode("PLN")
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country("Poland")
                        .city("Warsaw")
                        .street("Main Street")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("50.00"))
                .rooms(2)
                .floor(1)
                .propertyType("APARTMENT")
                .photoIds(List.of(UUID.randomUUID()))
                .build();

        PublishedListingsPageResponse response = PublishedListingsPageResponse.builder()
                .items(List.of(item))
                .totalElements(1)
                .totalPages(1)
                .page(0)
                .size(100)
                .build();

        when(currentUserProvider.requireCurrentJwt()).thenReturn(jwt);
        when(reindexProperties.getPageSize()).thenReturn(100);
        when(listingServiceClient.getPublishedListingsForReindex(0, 100, bearerToken)).thenReturn(response);
        doThrow(new RuntimeException("repository failure")).when(searchListingRepository).saveAll(anyList());

        // when + then
        ReindexFailedException exception = assertThrows(
                ReindexFailedException.class,
                () -> listingReindexService.reindexAllPublishedListings()
        );

        assertEquals("Failed to reindex published listings", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("repository failure", exception.getCause().getMessage());

        verify(searchListingRepository).saveAll(anyList());
    }
}