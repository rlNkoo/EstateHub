package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.events.producer.ListingEventsPublisher;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingMediaSyncServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PHOTO_ID_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTO_ID_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingVersionRepository versionRepository;

    @Mock
    private ListingEventsPublisher eventsPublisher;

    @InjectMocks
    private ListingMediaSyncService listingMediaSyncService;

    @Test
    void shouldIgnorePhotoUploadWhenListingDoesNotExist() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        verify(listingRepository).findById(listingId);
        verify(versionRepository, never()).findByListingIdAndVersionNo(any(), anyInt());
        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldIgnorePhotoDeleteWhenListingDoesNotExist() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // when
        listingMediaSyncService.onPhotoDeleted(listingId, mediaId);

        // then
        verify(listingRepository).findById(listingId);
        verify(versionRepository, never()).findByListingIdAndVersionNo(any(), anyInt());
        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldAddPhotoAndCreateNewVersionForDraftListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 2);
        ListingVersionEntity baseVersion = version(listingId, 2, List.of(PHOTO_ID_1));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(baseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        assertEquals(3, listing.getCurrentVersion());
        assertNull(listing.getPublishedVersion());

        ArgumentCaptor<ListingVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(listingId, savedVersion.getListingId());
        assertEquals(3, savedVersion.getVersionNo());
        assertEquals(List.of(PHOTO_ID_1, mediaId), savedVersion.getPhotoIds());
        assertEquals(baseVersion.getTitle(), savedVersion.getTitle());
        assertEquals(baseVersion.getDescription(), savedVersion.getDescription());
        assertEquals(baseVersion.getPriceAmount(), savedVersion.getPriceAmount());
        assertEquals(baseVersion.getCurrencyCode(), savedVersion.getCurrencyCode());
        assertEquals(baseVersion.getAddress(), savedVersion.getAddress());
        assertEquals(baseVersion.getArea(), savedVersion.getArea());
        assertEquals(baseVersion.getRooms(), savedVersion.getRooms());
        assertEquals(baseVersion.getFloor(), savedVersion.getFloor());
        assertEquals(baseVersion.getPropertyType(), savedVersion.getPropertyType());

        verify(listingRepository).save(listing);
        verify(eventsPublisher, never()).publishListingUpdated(any(), any());
    }

    @Test
    void shouldAddPhotoAndPublishUpdateForPublishedListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = publishedListing(listingId, OWNER_ID, 4, 3);
        ListingVersionEntity publishedBaseVersion = version(listingId, 3, List.of(PHOTO_ID_1));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 3))
                .thenReturn(Optional.of(publishedBaseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        assertEquals(5, listing.getCurrentVersion());
        assertEquals(5, listing.getPublishedVersion());

        ArgumentCaptor<ListingVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(5, savedVersion.getVersionNo());
        assertEquals(List.of(PHOTO_ID_1, mediaId), savedVersion.getPhotoIds());

        ArgumentCaptor<ListingUpdatedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingUpdatedPayload.class);
        verify(eventsPublisher).publishListingUpdated(eq(listingId), payloadCaptor.capture());

        ListingUpdatedPayload payload = payloadCaptor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(OWNER_ID, payload.ownerId());
        assertEquals("PUBLISHED", payload.status());
        assertEquals(5, payload.version());
        assertEquals("Apartment title", payload.title());
        assertEquals("Apartment description", payload.description());
        assertEquals(new BigDecimal("650000.00"), payload.priceAmount());
        assertEquals("PLN", payload.currencyCode());
        assertNotNull(payload.address());
        assertEquals("Poland", payload.address().country());
        assertEquals("Warsaw", payload.address().city());
        assertEquals("APARTMENT", payload.propertyType());
        assertEquals(List.of(PHOTO_ID_1, mediaId), payload.photoIds());
    }

    @Test
    void shouldRemovePhotoAndCreateNewVersionForDraftListing() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 2);
        ListingVersionEntity baseVersion = version(listingId, 2, List.of(PHOTO_ID_1, PHOTO_ID_2));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(baseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoDeleted(listingId, PHOTO_ID_2);

        // then
        assertEquals(3, listing.getCurrentVersion());

        ArgumentCaptor<ListingVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(List.of(PHOTO_ID_1), savedVersion.getPhotoIds());

        verify(eventsPublisher, never()).publishListingUpdated(any(), any());
    }

    @Test
    void shouldDoNothingWhenAddingAlreadyExistingPhoto() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 2);
        ListingVersionEntity baseVersion = version(listingId, 2, List.of(PHOTO_ID_1, PHOTO_ID_2));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(baseVersion));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, PHOTO_ID_1);

        // then
        assertEquals(2, listing.getCurrentVersion());

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldDoNothingWhenRemovingMissingPhoto() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID missingPhotoId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 2);
        ListingVersionEntity baseVersion = version(listingId, 2, List.of(PHOTO_ID_1, PHOTO_ID_2));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(baseVersion));

        // when
        listingMediaSyncService.onPhotoDeleted(listingId, missingPhotoId);

        // then
        assertEquals(2, listing.getCurrentVersion());

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenBaseVersionNotFoundForDraftListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 2);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.empty());

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listingMediaSyncService.onPhotoUploaded(listingId, mediaId)
        );

        assertEquals(
                "Listing version not found for listingId=" + listingId + " versionNo=2",
                exception.getMessage()
        );

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenBaseVersionNotFoundForPublishedListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = publishedListing(listingId, OWNER_ID, 5, 3);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 3))
                .thenReturn(Optional.empty());

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listingMediaSyncService.onPhotoUploaded(listingId, mediaId)
        );

        assertEquals(
                "Listing version not found for listingId=" + listingId + " versionNo=3",
                exception.getMessage()
        );

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldLoadPublishedVersionAsBaseForPublishedListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = publishedListing(listingId, OWNER_ID, 7, 4);
        ListingVersionEntity publishedBaseVersion = version(listingId, 4, List.of(PHOTO_ID_1));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 4))
                .thenReturn(Optional.of(publishedBaseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        verify(versionRepository).findByListingIdAndVersionNo(listingId, 4);

        ArgumentCaptor<ListingVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(8, savedVersion.getVersionNo());
        assertEquals(List.of(PHOTO_ID_1, mediaId), savedVersion.getPhotoIds());
    }

    @Test
    void shouldLoadCurrentVersionAsBaseForDraftListing() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = draftListing(listingId, OWNER_ID, 6);
        ListingVersionEntity currentBaseVersion = version(listingId, 6, List.of(PHOTO_ID_1));

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 6))
                .thenReturn(Optional.of(currentBaseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        verify(versionRepository).findByListingIdAndVersionNo(listingId, 6);

        ArgumentCaptor<ListingVersionEntity> versionCaptor =
                ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(7, savedVersion.getVersionNo());
        assertEquals(List.of(PHOTO_ID_1, mediaId), savedVersion.getPhotoIds());
    }

    @Test
    void shouldBuildUpdatedPayloadWithNullSafeAddressAndPropertyType() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .ownerId(OWNER_ID)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(2)
                .publishedVersion(2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();

        ListingVersionEntity baseVersion = ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(2)
                .title("Title without address")
                .description("Description without address")
                .priceAmount(new BigDecimal("100000.00"))
                .currencyCode("PLN")
                .address(null)
                .area(new BigDecimal("30.00"))
                .rooms(1)
                .floor(0)
                .propertyType(null)
                .photoIds(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(baseVersion));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingMediaSyncService.onPhotoUploaded(listingId, mediaId);

        // then
        ArgumentCaptor<ListingUpdatedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingUpdatedPayload.class);
        verify(eventsPublisher).publishListingUpdated(eq(listingId), payloadCaptor.capture());

        ListingUpdatedPayload payload = payloadCaptor.getValue();
        assertNotNull(payload.address());
        assertNull(payload.address().country());
        assertNull(payload.address().city());
        assertNull(payload.address().street());
        assertNull(payload.address().postalCode());
        assertNull(payload.propertyType());
        assertEquals(List.of(mediaId), payload.photoIds());
    }

    private ListingEntity draftListing(UUID listingId, UUID ownerId, int currentVersion) {
        return ListingEntity.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status(ListingStatus.DRAFT)
                .currentVersion(currentVersion)
                .publishedVersion(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(null)
                .build();
    }

    private ListingEntity publishedListing(
            UUID listingId,
            UUID ownerId,
            int currentVersion,
            Integer publishedVersion
    ) {
        return ListingEntity.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(currentVersion)
                .publishedVersion(publishedVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();
    }

    private ListingVersionEntity version(UUID listingId, int versionNo, List<UUID> photoIds) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title("Apartment title")
                .description("Apartment description")
                .priceAmount(new BigDecimal("650000.00"))
                .currencyCode("PLN")
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country("Poland")
                        .city("Warsaw")
                        .street("Main Street 10")
                        .postalCode("00-100")
                        .build())
                .area(new BigDecimal("60.00"))
                .rooms(3)
                .floor(2)
                .propertyType(com.rlnkoo.listingservice.domain.model.PropertyType.APARTMENT)
                .photoIds(new ArrayList<>(photoIds))
                .createdAt(Instant.now())
                .build();
    }
}