package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.api.listings.dto.UpdateListingRequest;
import com.rlnkoo.listingservice.domain.exception.*;
import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.domain.model.PropertyType;
import com.rlnkoo.listingservice.events.producer.ListingEventsPublisher;
import com.rlnkoo.listingservice.events.types.ListingArchivedPayload;
import com.rlnkoo.listingservice.events.types.ListingPublishedPayload;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import com.rlnkoo.listingservice.security.CurrentUser;
import com.rlnkoo.listingservice.security.CurrentUserProvider;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHOTO_ID_1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PHOTO_ID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingVersionRepository versionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ListingEventsPublisher eventsPublisher;

    @InjectMocks
    private ListingService listingService;

    @Test
    void shouldCreateDraftForAuthenticatedUser() {
        // given
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UUID listingId = listingService.createDraft();

        // then
        assertNotNull(listingId);

        ArgumentCaptor<ListingEntity> listingCaptor = ArgumentCaptor.forClass(ListingEntity.class);
        verify(listingRepository).save(listingCaptor.capture());
        verify(currentUserProvider).requireCurrentUser();
        verifyNoInteractions(versionRepository, eventsPublisher);

        ListingEntity saved = listingCaptor.getValue();
        assertEquals(listingId, saved.getId());
        assertEquals(USER_ID, saved.getOwnerId());
        assertEquals(ListingStatus.DRAFT, saved.getStatus());
        assertEquals(1, saved.getCurrentVersion());
        assertNull(saved.getPublishedVersion());
        assertNull(saved.getPublishedAt());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenCreatingDraftWithoutAuthenticatedUser() {
        // given
        when(currentUserProvider.requireCurrentUser()).thenThrow(new AuthenticationRequiredException());

        // when + then
        assertThrows(AuthenticationRequiredException.class, () -> listingService.createDraft());

        verify(currentUserProvider).requireCurrentUser();
        verifyNoInteractions(listingRepository, versionRepository, eventsPublisher);
    }

    @Test
    void shouldUpdateDraftListingAndCreateNewVersionWithoutPublishingEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 1);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ListingEntity result = listingService.update(listingId, validUpdateRequest());

        // then
        assertSame(listing, result);
        assertEquals(2, listing.getCurrentVersion());
        assertNull(listing.getPublishedVersion());

        ArgumentCaptor<ListingVersionEntity> versionCaptor = ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertEquals(listingId, savedVersion.getListingId());
        assertEquals(2, savedVersion.getVersionNo());
        assertEquals("Modern apartment in city center", savedVersion.getTitle());
        assertEquals("Nice flat for sale", savedVersion.getDescription());
        assertEquals(new BigDecimal("550000.00"), savedVersion.getPriceAmount());
        assertEquals("PLN", savedVersion.getCurrencyCode());
        assertNotNull(savedVersion.getAddress());
        assertEquals("England", savedVersion.getAddress().getCountry());
        assertEquals("London", savedVersion.getAddress().getCity());
        assertEquals("Main Street 1", savedVersion.getAddress().getStreet());
        assertEquals("00-001", savedVersion.getAddress().getPostalCode());
        assertEquals(new BigDecimal("52.50"), savedVersion.getArea());
        assertEquals(3, savedVersion.getRooms());
        assertEquals(4, savedVersion.getFloor());
        assertEquals(PropertyType.APARTMENT, savedVersion.getPropertyType());
        assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), savedVersion.getPhotoIds());

        verify(listingRepository).save(listing);
        verify(eventsPublisher, never()).publishListingUpdated(any(), any());
        verify(eventsPublisher, never()).publishListingPublished(any(), any());
        verify(eventsPublisher, never()).publishListingArchived(any(), any());
    }

    @Test
    void shouldUseEmptyPhotoIdsWhenUpdatingAndRequestPhotoIdsIsNull() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 1);

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Modern apartment in city center")
                .description("Nice flat for sale")
                .priceAmount(new BigDecimal("550000.00"))
                .currencyCode("PLN")
                .address(UpdateListingRequest.AddressRequest.builder()
                        .country("England")
                        .city("London")
                        .street("Main Street 1")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("52.50"))
                .rooms(3)
                .floor(4)
                .propertyType("APARTMENT")
                .photoIds(null)
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingService.update(listingId, request);

        // then
        ArgumentCaptor<ListingVersionEntity> versionCaptor = ArgumentCaptor.forClass(ListingVersionEntity.class);
        verify(versionRepository).save(versionCaptor.capture());

        ListingVersionEntity savedVersion = versionCaptor.getValue();
        assertNotNull(savedVersion.getPhotoIds());
        assertTrue(savedVersion.getPhotoIds().isEmpty());

        verify(eventsPublisher, never()).publishListingUpdated(any(), any());
    }

    @Test
    void shouldUpdatePublishedListingLiveAndPublishListingUpdatedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = publishedListing(listingId, USER_ID, 2, 2, Instant.now());

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ListingEntity result = listingService.update(listingId, validUpdateRequest());

        // then
        assertSame(listing, result);
        assertEquals(3, listing.getCurrentVersion());
        assertEquals(3, listing.getPublishedVersion());

        ArgumentCaptor<ListingUpdatedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingUpdatedPayload.class);

        verify(eventsPublisher).publishListingUpdated(eq(listingId), payloadCaptor.capture());

        ListingUpdatedPayload payload = payloadCaptor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(USER_ID, payload.ownerId());
        assertEquals("PUBLISHED", payload.status());
        assertEquals(3, payload.version());
        assertEquals("Modern apartment in city center", payload.title());
        assertEquals("Nice flat for sale", payload.description());
        assertEquals(new BigDecimal("550000.00"), payload.priceAmount());
        assertEquals("PLN", payload.currencyCode());
        assertNotNull(payload.address());
        assertEquals("England", payload.address().country());
        assertEquals("London", payload.address().city());
        assertEquals("Main Street 1", payload.address().street());
        assertEquals("00-001", payload.address().postalCode());
        assertEquals(new BigDecimal("52.50"), payload.area());
        assertEquals(3, payload.rooms());
        assertEquals(4, payload.floor());
        assertEquals("APARTMENT", payload.propertyType());
        assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), payload.photoIds());
    }

    @Test
    void shouldThrowListingNotFoundExceptionWhenUpdatingMissingListing() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // when + then
        ListingNotFoundException exception = assertThrows(
                ListingNotFoundException.class,
                () -> listingService.update(listingId, validUpdateRequest())
        );

        assertEquals("Listing not found: " + listingId, exception.getMessage());

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowListingOwnershipExceptionWhenUpdatingForeignListingAsNonAdmin() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, OTHER_USER_ID, 1);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when + then
        ListingOwnershipException exception = assertThrows(
                ListingOwnershipException.class,
                () -> listingService.update(listingId, validUpdateRequest())
        );

        assertEquals("Access denied for listing: " + listingId, exception.getMessage());

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldAllowAdminToUpdateForeignListing() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser admin = currentUser(USER_ID, Set.of("ADMIN"));
        ListingEntity listing = draftListing(listingId, OTHER_USER_ID, 1);

        when(currentUserProvider.requireCurrentUser()).thenReturn(admin);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ListingEntity result = listingService.update(listingId, validUpdateRequest());

        // then
        assertSame(listing, result);
        assertEquals(2, listing.getCurrentVersion());

        verify(versionRepository).save(any(ListingVersionEntity.class));
        verify(listingRepository).save(listing);
    }

    @Test
    void shouldThrowListingNotEditableExceptionWhenUpdatingArchivedListing() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = archivedListing(listingId, USER_ID, 2);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when + then
        ListingNotEditableException exception = assertThrows(
                ListingNotEditableException.class,
                () -> listingService.update(listingId, validUpdateRequest())
        );

        assertEquals(
                "Listing is not editable in status ARCHIVED (id=" + listingId + ")",
                exception.getMessage()
        );

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowInvalidPropertyTypeExceptionWhenUpdatingWithInvalidPropertyType() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 1);

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title("Modern apartment in city center")
                .description("Nice flat for sale")
                .priceAmount(new BigDecimal("550000.00"))
                .currencyCode("PLN")
                .address(UpdateListingRequest.AddressRequest.builder()
                        .country("England")
                        .city("London")
                        .street("Main Street 1")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("52.50"))
                .rooms(3)
                .floor(4)
                .propertyType("CASTLE")
                .photoIds(List.of(PHOTO_ID_1))
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when + then
        InvalidPropertyTypeException exception = assertThrows(
                InvalidPropertyTypeException.class,
                () -> listingService.update(listingId, request)
        );

        assertEquals("Invalid propertyType: CASTLE", exception.getMessage());

        verify(versionRepository, never()).save(any());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowListingNotPublishableExceptionWhenLiveUpdateHasMissingTitle() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = publishedListing(listingId, USER_ID, 2, 2, Instant.now());

        UpdateListingRequest request = UpdateListingRequest.builder()
                .title(" ")
                .description("Nice flat for sale")
                .priceAmount(new BigDecimal("550000.00"))
                .currencyCode("PLN")
                .address(UpdateListingRequest.AddressRequest.builder()
                        .country("England")
                        .city("London")
                        .street("Main Street 1")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("52.50"))
                .rooms(3)
                .floor(4)
                .propertyType("APARTMENT")
                .photoIds(List.of(PHOTO_ID_1))
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.save(any(ListingVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when + then
        ListingNotPublishableException exception = assertThrows(
                ListingNotPublishableException.class,
                () -> listingService.update(listingId, request)
        );

        assertEquals(
                "Listing cannot be published (id=" + listingId + "): title missing",
                exception.getMessage()
        );

        verify(versionRepository).save(any(ListingVersionEntity.class));
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldPublishDraftListingWhenCurrentVersionExistsAndIsPublishable() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 2);
        ListingVersionEntity currentVersion = publishableVersion(listingId, 2);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(currentVersion));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ListingEntity result = listingService.publish(listingId);

        // then
        assertSame(listing, result);
        assertEquals(ListingStatus.PUBLISHED, listing.getStatus());
        assertEquals(2, listing.getPublishedVersion());
        assertNotNull(listing.getPublishedAt());

        ArgumentCaptor<ListingPublishedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingPublishedPayload.class);

        verify(eventsPublisher).publishListingPublished(eq(listingId), payloadCaptor.capture());

        ListingPublishedPayload payload = payloadCaptor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(USER_ID, payload.ownerId());
        assertEquals("PUBLISHED", payload.status());
        assertEquals(2, payload.version());
        assertNotNull(payload.publishedAt());
        assertEquals("Beautiful apartment", payload.title());
        assertEquals("Spacious and sunny", payload.description());
        assertEquals(new BigDecimal("700000.00"), payload.priceAmount());
        assertEquals("PLN", payload.currencyCode());
        assertNotNull(payload.address());
        assertEquals("England", payload.address().country());
        assertEquals("Manchester", payload.address().city());
        assertEquals("APARTMENT", payload.propertyType());
        assertEquals(List.of(PHOTO_ID_1, PHOTO_ID_2), payload.photoIds());
    }

    @Test
    void shouldThrowListingContentNotFoundExceptionWhenPublishingWithoutCurrentVersionContent() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 2);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.empty());

        // when + then
        ListingContentNotFoundException exception = assertThrows(
                ListingContentNotFoundException.class,
                () -> listingService.publish(listingId)
        );

        assertEquals(
                "Listing content not found for listing: " + listingId,
                exception.getMessage()
        );

        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldThrowInvalidListingStatusTransitionExceptionWhenPublishingAlreadyPublishedListing() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = publishedListing(listingId, USER_ID, 2, 2, Instant.now());

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when + then
        InvalidListingStatusTransitionException exception = assertThrows(
                InvalidListingStatusTransitionException.class,
                () -> listingService.publish(listingId)
        );

        assertEquals(
                "Invalid listing status transition for " + listingId + ": PUBLISHED -> PUBLISHED",
                exception.getMessage()
        );

        verify(versionRepository, never()).findByListingIdAndVersionNo(any(), anyInt());
        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldArchivePublishedListingAndPublishArchivedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = publishedListing(listingId, USER_ID, 4, 3, Instant.now());

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ListingEntity result = listingService.archive(listingId);

        // then
        assertSame(listing, result);
        assertEquals(ListingStatus.ARCHIVED, listing.getStatus());

        ArgumentCaptor<ListingArchivedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingArchivedPayload.class);

        verify(eventsPublisher).publishListingArchived(eq(listingId), payloadCaptor.capture());

        ListingArchivedPayload payload = payloadCaptor.getValue();
        assertEquals(listingId, payload.listingId());
        assertEquals(USER_ID, payload.ownerId());
        assertEquals("ARCHIVED", payload.status());
        assertEquals(3, payload.version());
        assertNotNull(payload.archivedAt());
    }

    @Test
    void shouldUseCurrentVersionInArchiveEventWhenPublishedVersionIsNull() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .ownerId(USER_ID)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(5)
                .publishedVersion(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(ListingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        listingService.archive(listingId);

        // then
        ArgumentCaptor<ListingArchivedPayload> payloadCaptor =
                ArgumentCaptor.forClass(ListingArchivedPayload.class);

        verify(eventsPublisher).publishListingArchived(eq(listingId), payloadCaptor.capture());
        assertEquals(5, payloadCaptor.getValue().version());
    }

    @Test
    void shouldThrowInvalidListingStatusTransitionExceptionWhenArchivingDraftListing() {
        // given
        UUID listingId = UUID.randomUUID();
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        ListingEntity listing = draftListing(listingId, USER_ID, 1);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when + then
        InvalidListingStatusTransitionException exception = assertThrows(
                InvalidListingStatusTransitionException.class,
                () -> listingService.archive(listingId)
        );

        assertEquals(
                "Invalid listing status transition for " + listingId + ": DRAFT -> ARCHIVED",
                exception.getMessage()
        );

        verify(listingRepository, never()).save(any(ListingEntity.class));
        verifyNoInteractions(eventsPublisher);
    }

    @Test
    void shouldReturnPublishedVersionWhenResolvingVersionForPublishedListing() {
        // given
        ListingEntity listing = publishedListing(UUID.randomUUID(), USER_ID, 5, 3, Instant.now());

        // when
        int version = listingService.resolveVersionForRead(listing);

        // then
        assertEquals(3, version);
    }

    @Test
    void shouldReturnCurrentVersionWhenResolvingVersionForDraftListing() {
        // given
        ListingEntity listing = draftListing(UUID.randomUUID(), USER_ID, 4);

        // when
        int version = listingService.resolveVersionForRead(listing);

        // then
        assertEquals(4, version);
    }

    @Test
    void shouldReturnCurrentVersionWhenPublishedButPublishedVersionIsNull() {
        // given
        ListingEntity listing = ListingEntity.builder()
                .id(UUID.randomUUID())
                .ownerId(USER_ID)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(7)
                .publishedVersion(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();

        // when
        int version = listingService.resolveVersionForRead(listing);

        // then
        assertEquals(7, version);
    }

    @Test
    void shouldReturnMyListingsForCurrentUser() {
        // given
        CurrentUser user = currentUser(USER_ID, Set.of("USER"));
        List<ListingEntity> expected = List.of(
                draftListing(UUID.randomUUID(), USER_ID, 1),
                publishedListing(UUID.randomUUID(), USER_ID, 3, 2, Instant.now())
        );

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(listingRepository.findAllByOwnerIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(expected);

        // when
        List<ListingEntity> result = listingService.getMyListings();

        // then
        assertEquals(expected, result);

        verify(currentUserProvider).requireCurrentUser();
        verify(listingRepository).findAllByOwnerIdOrderByUpdatedAtDesc(USER_ID);
    }

    @Test
    void shouldReturnListingWhenGetListingCalled() {
        // given
        UUID listingId = UUID.randomUUID();
        ListingEntity listing = draftListing(listingId, USER_ID, 1);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // when
        Optional<ListingEntity> result = listingService.getListing(listingId);

        // then
        assertTrue(result.isPresent());
        assertSame(listing, result.get());
        verify(listingRepository).findById(listingId);
    }

    @Test
    void shouldReturnListingVersionWhenGetListingVersionCalled() {
        // given
        UUID listingId = UUID.randomUUID();
        ListingVersionEntity version = publishableVersion(listingId, 2);

        when(versionRepository.findByListingIdAndVersionNo(listingId, 2))
                .thenReturn(Optional.of(version));

        // when
        Optional<ListingVersionEntity> result = listingService.getListingVersion(listingId, 2);

        // then
        assertTrue(result.isPresent());
        assertSame(version, result.get());
        verify(versionRepository).findByListingIdAndVersionNo(listingId, 2);
    }

    private UpdateListingRequest validUpdateRequest() {
        return UpdateListingRequest.builder()
                .title("Modern apartment in city center")
                .description("Nice flat for sale")
                .priceAmount(new BigDecimal("550000.00"))
                .currencyCode("PLN")
                .address(UpdateListingRequest.AddressRequest.builder()
                        .country("England")
                        .city("London")
                        .street("Main Street 1")
                        .postalCode("00-001")
                        .build())
                .area(new BigDecimal("52.50"))
                .rooms(3)
                .floor(4)
                .propertyType("APARTMENT")
                .photoIds(List.of(PHOTO_ID_1, PHOTO_ID_2))
                .build();
    }

    private ListingVersionEntity publishableVersion(UUID listingId, int versionNo) {
        return ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(versionNo)
                .title("Beautiful apartment")
                .description("Spacious and sunny")
                .priceAmount(new BigDecimal("700000.00"))
                .currencyCode("PLN")
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country("England")
                        .city("Manchester")
                        .street("Sunny Street 15")
                        .postalCode("30-001")
                        .build())
                .area(new BigDecimal("68.00"))
                .rooms(4)
                .floor(2)
                .propertyType(PropertyType.APARTMENT)
                .photoIds(List.of(PHOTO_ID_1, PHOTO_ID_2))
                .createdAt(Instant.now())
                .build();
    }

    private CurrentUser currentUser(UUID userId, Set<String> roles) {
        return CurrentUser.builder()
                .userId(userId)
                .email("user@example.com")
                .roles(roles)
                .build();
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
            Integer publishedVersion,
            Instant publishedAt
    ) {
        return ListingEntity.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status(ListingStatus.PUBLISHED)
                .currentVersion(currentVersion)
                .publishedVersion(publishedVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(publishedAt)
                .build();
    }

    private ListingEntity archivedListing(UUID listingId, UUID ownerId, int currentVersion) {
        return ListingEntity.builder()
                .id(listingId)
                .ownerId(ownerId)
                .status(ListingStatus.ARCHIVED)
                .currentVersion(currentVersion)
                .publishedVersion(currentVersion)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .publishedAt(Instant.now())
                .build();
    }
}