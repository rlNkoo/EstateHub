package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.api.admin.dto.PublishedListingForReindexResponse;
import com.rlnkoo.listingservice.api.admin.dto.PublishedListingsPageResponse;
import com.rlnkoo.listingservice.domain.exception.InvalidAdminListingQueryException;
import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.domain.model.PropertyType;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminListingQueryServiceTest {

    private static final UUID LISTING_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LISTING_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OWNER_ID_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PHOTO_ID_1 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PHOTO_ID_2 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PHOTO_ID_3 = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final Instant PUBLISHED_AT_1 = Instant.parse("2025-01-10T10:15:30Z");
    private static final Instant PUBLISHED_AT_2 = Instant.parse("2025-01-09T08:00:00Z");
    private static final Instant UPDATED_AT_1 = Instant.parse("2025-01-11T12:00:00Z");
    private static final Instant UPDATED_AT_2 = Instant.parse("2025-01-10T15:30:00Z");

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingVersionRepository listingVersionRepository;

    @InjectMocks
    private AdminListingQueryService adminListingQueryService;

    @Test
    void shouldReturnPublishedListingsPageForReindex() {
        // given
        ListingEntity listing1 = publishedListing(LISTING_ID_1, OWNER_ID_1, 5, 3, PUBLISHED_AT_1, UPDATED_AT_1);
        ListingEntity listing2 = publishedListing(LISTING_ID_2, OWNER_ID_2, 4, 2, PUBLISHED_AT_2, UPDATED_AT_2);

        ListingVersionEntity version1 = publishedVersion(
                LISTING_ID_1,
                3,
                "Luxury apartment in city center",
                "Spacious apartment with balcony",
                new BigDecimal("950000.00"),
                "PLN",
                "Poland",
                "Warsaw",
                "Main Street 15",
                "00-100",
                new BigDecimal("82.50"),
                4,
                6,
                PropertyType.APARTMENT,
                List.of(PHOTO_ID_1, PHOTO_ID_2)
        );

        ListingVersionEntity version2 = publishedVersion(
                LISTING_ID_2,
                2,
                "House with garden",
                "Detached house in quiet neighborhood",
                new BigDecimal("1250000.00"),
                "PLN",
                "Poland",
                "Krakow",
                "Garden Street 7",
                "30-200",
                new BigDecimal("140.00"),
                5,
                1,
                PropertyType.HOUSE,
                List.of(PHOTO_ID_3)
        );

        Page<ListingEntity> listingsPage = new PageImpl<>(List.of(listing1, listing2));

        when(listingRepository.findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(listingsPage);
        when(listingVersionRepository.findByListingIdAndVersionNo(LISTING_ID_1, 3))
                .thenReturn(Optional.of(version1));
        when(listingVersionRepository.findByListingIdAndVersionNo(LISTING_ID_2, 2))
                .thenReturn(Optional.of(version2));

        // when
        PublishedListingsPageResponse result = adminListingQueryService.getPublishedListingsForReindex(0, 100);

        // then
        assertNotNull(result);
        assertEquals(2, result.items().size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(2, result.size());

        PublishedListingForReindexResponse item1 = result.items().get(0);
        assertEquals(LISTING_ID_1, item1.id());
        assertEquals(OWNER_ID_1, item1.ownerId());
        assertEquals("PUBLISHED", item1.status());
        assertEquals(3, item1.version());
        assertEquals(PUBLISHED_AT_1, item1.publishedAt());
        assertEquals(UPDATED_AT_1, item1.updatedAt());
        assertEquals("Luxury apartment in city center", item1.title());
        assertEquals("Spacious apartment with balcony", item1.description());
        assertEquals(new BigDecimal("950000.00"), item1.priceAmount());
        assertEquals("PLN", item1.currencyCode());
        assertNotNull(item1.address());
        assertEquals("Poland", item1.address().country());
        assertEquals("Warsaw", item1.address().city());
        assertEquals("Main Street 15", item1.address().street());
        assertEquals("00-100", item1.address().postalCode());
        assertEquals(new BigDecimal("82.50"), item1.area());
        assertEquals(4, item1.rooms());
        assertEquals(6, item1.floor());
        assertEquals("APARTMENT", item1.propertyType());
        assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), item1.photoIds());

        PublishedListingForReindexResponse item2 = result.items().get(1);
        assertEquals(LISTING_ID_2, item2.id());
        assertEquals(OWNER_ID_2, item2.ownerId());
        assertEquals("PUBLISHED", item2.status());
        assertEquals(2, item2.version());
        assertEquals(PUBLISHED_AT_2, item2.publishedAt());
        assertEquals(UPDATED_AT_2, item2.updatedAt());
        assertEquals("House with garden", item2.title());
        assertEquals("Detached house in quiet neighborhood", item2.description());
        assertEquals(new BigDecimal("1250000.00"), item2.priceAmount());
        assertEquals("PLN", item2.currencyCode());
        assertNotNull(item2.address());
        assertEquals("Poland", item2.address().country());
        assertEquals("Krakow", item2.address().city());
        assertEquals("Garden Street 7", item2.address().street());
        assertEquals("30-200", item2.address().postalCode());
        assertEquals(new BigDecimal("140.00"), item2.area());
        assertEquals(5, item2.rooms());
        assertEquals(1, item2.floor());
        assertEquals("HOUSE", item2.propertyType());
        assertEquals(List.of(PHOTO_ID_3), item2.photoIds());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(listingRepository).findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());

        verify(listingVersionRepository).findByListingIdAndVersionNo(LISTING_ID_1, 3);
        verify(listingVersionRepository).findByListingIdAndVersionNo(LISTING_ID_2, 2);
    }

    @Test
    void shouldSkipPublishedListingWithoutPublishedVersion() {
        // given
        ListingEntity listingWithoutPublishedVersion = ListingEntity.builder()
                .id(LISTING_ID_1)
                .ownerId(OWNER_ID_1)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(5)
                .publishedVersion(null)
                .createdAt(Instant.now())
                .updatedAt(UPDATED_AT_1)
                .publishedAt(PUBLISHED_AT_1)
                .build();

        Page<ListingEntity> listingsPage = new PageImpl<>(List.of(listingWithoutPublishedVersion));

        when(listingRepository.findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(listingsPage);

        // when
        PublishedListingsPageResponse result = adminListingQueryService.getPublishedListingsForReindex(0, 50);

        // then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(1, result.size());

        verify(listingVersionRepository, never()).findByListingIdAndVersionNo(any(), anyInt());
    }

    @Test
    void shouldSkipPublishedListingWhenPublishedVersionContentMissing() {
        // given
        ListingEntity listing = publishedListing(LISTING_ID_1, OWNER_ID_1, 5, 3, PUBLISHED_AT_1, UPDATED_AT_1);
        Page<ListingEntity> listingsPage = new PageImpl<>(List.of(listing));

        when(listingRepository.findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(listingsPage);
        when(listingVersionRepository.findByListingIdAndVersionNo(LISTING_ID_1, 3))
                .thenReturn(Optional.empty());

        // when
        PublishedListingsPageResponse result = adminListingQueryService.getPublishedListingsForReindex(0, 50);

        // then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(1, result.size());

        verify(listingVersionRepository).findByListingIdAndVersionNo(LISTING_ID_1, 3);
    }

    @Test
    void shouldReturnEmptyItemsWhenPageHasNoListings() {
        // given
        Page<ListingEntity> emptyPage = Page.empty();

        when(listingRepository.findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when
        PublishedListingsPageResponse result = adminListingQueryService.getPublishedListingsForReindex(0, 100);

        // then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(0, result.page());
        assertEquals(0, result.size());

        verify(listingVersionRepository, never()).findByListingIdAndVersionNo(any(), anyInt());
    }

    @Test
    void shouldMapNullAddressAndNullPropertyTypeSafely() {
        // given
        ListingEntity listing = publishedListing(LISTING_ID_1, OWNER_ID_1, 3, 2, PUBLISHED_AT_1, UPDATED_AT_1);

        ListingVersionEntity version = ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(LISTING_ID_1)
                .versionNo(2)
                .title("Listing without address")
                .description("Description")
                .priceAmount(new BigDecimal("500000.00"))
                .currencyCode("PLN")
                .address(null)
                .area(new BigDecimal("55.00"))
                .rooms(2)
                .floor(1)
                .propertyType(null)
                .photoIds(null)
                .createdAt(Instant.now())
                .build();

        Page<ListingEntity> listingsPage = new PageImpl<>(List.of(listing));

        when(listingRepository.findAllByStatusOrderByPublishedAtDesc(eq(ListingStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(listingsPage);
        when(listingVersionRepository.findByListingIdAndVersionNo(LISTING_ID_1, 2))
                .thenReturn(Optional.of(version));

        // when
        PublishedListingsPageResponse result = adminListingQueryService.getPublishedListingsForReindex(0, 20);

        // then
        assertEquals(1, result.items().size());

        PublishedListingForReindexResponse item = result.items().getFirst();
        assertNotNull(item.address());
        assertNull(item.address().country());
        assertNull(item.address().city());
        assertNull(item.address().street());
        assertNull(item.address().postalCode());
        assertNull(item.propertyType());
        assertNotNull(item.photoIds());
        assertTrue(item.photoIds().isEmpty());
    }

    @Test
    void shouldThrowInvalidAdminListingQueryExceptionWhenPageIsNegative() {
        // when + then
        InvalidAdminListingQueryException exception = assertThrows(
                InvalidAdminListingQueryException.class,
                () -> adminListingQueryService.getPublishedListingsForReindex(-1, 100)
        );

        assertEquals("page must be greater than or equal to 0", exception.getMessage());

        verifyNoInteractions(listingRepository, listingVersionRepository);
    }

    @Test
    void shouldThrowInvalidAdminListingQueryExceptionWhenSizeIsLessThanOne() {
        // when + then
        InvalidAdminListingQueryException exception = assertThrows(
                InvalidAdminListingQueryException.class,
                () -> adminListingQueryService.getPublishedListingsForReindex(0, 0)
        );

        assertEquals("size must be greater than 0", exception.getMessage());

        verifyNoInteractions(listingRepository, listingVersionRepository);
    }

    @Test
    void shouldThrowInvalidAdminListingQueryExceptionWhenSizeIsGreaterThanFiveHundred() {
        // when + then
        InvalidAdminListingQueryException exception = assertThrows(
                InvalidAdminListingQueryException.class,
                () -> adminListingQueryService.getPublishedListingsForReindex(0, 501)
        );

        assertEquals("size must be less than or equal to 500", exception.getMessage());

        verifyNoInteractions(listingRepository, listingVersionRepository);
    }

    private ListingEntity publishedListing(
            UUID listingId,
            UUID ownerId,
            int currentVersion,
            Integer publishedVersion,
            Instant publishedAt,
            Instant updatedAt
    ) {
        return ListingEntity.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(currentVersion)
                .publishedVersion(publishedVersion)
                .createdAt(Instant.now())
                .updatedAt(updatedAt)
                .publishedAt(publishedAt)
                .build();
    }

    private ListingVersionEntity publishedVersion(
            UUID listingId,
            int versionNo,
            String title,
            String description,
            BigDecimal priceAmount,
            String currencyCode,
            String country,
            String city,
            String street,
            String postalCode,
            BigDecimal area,
            Integer rooms,
            Integer floor,
            PropertyType propertyType,
            List<UUID> photoIds
    ) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title(title)
                .description(description)
                .priceAmount(priceAmount)
                .currencyCode(currencyCode)
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country(country)
                        .city(city)
                        .street(street)
                        .postalCode(postalCode)
                        .build())
                .area(area)
                .rooms(rooms)
                .floor(floor)
                .propertyType(propertyType)
                .photoIds(photoIds)
                .createdAt(Instant.now())
                .build();
    }
}