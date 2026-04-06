package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.ExternalServiceException;
import com.rlnkoo.mediaservice.domain.exception.ListingOwnershipException;
import com.rlnkoo.mediaservice.integration.listing.ListingServiceClient;
import com.rlnkoo.mediaservice.integration.listing.dto.ListingDetailsDto;
import com.rlnkoo.mediaservice.security.CurrentUser;
import com.rlnkoo.mediaservice.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestListingOwnershipVerifierTest {

    @Mock
    private ListingServiceClient listingServiceClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private RestListingOwnershipVerifier verifier;

    @Test
    void shouldReturnListingOwnerIdWhenRequesterIsOwner() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(ownerId)
                .email("owner@example.com")
                .roles(Set.of("USER"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when
        UUID result = verifier.requireOwnerOrAdmin(listingId);

        // then
        assertEquals(ownerId, result);
        verify(currentUserProvider).requireCurrentUser();
        verify(listingServiceClient).getListing(listingId);
    }

    @Test
    void shouldReturnListingOwnerIdWhenRequesterIsAdmin() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser adminUser = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("admin@example.com")
                .roles(Set.of("ADMIN"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(currentUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when
        UUID result = verifier.requireOwnerOrAdmin(listingId);

        // then
        assertEquals(ownerId, result);
        verify(currentUserProvider).requireCurrentUser();
        verify(listingServiceClient).getListing(listingId);
    }

    @Test
    void shouldThrowListingOwnershipExceptionWhenRequesterIsNotOwnerAndNotAdmin() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser stranger = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("stranger@example.com")
                .roles(Set.of("USER"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(currentUserProvider.requireCurrentUser()).thenReturn(stranger);
        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when + then
        ListingOwnershipException exception = assertThrows(
                ListingOwnershipException.class,
                () -> verifier.requireOwnerOrAdmin(listingId)
        );

        assertEquals("Access denied for listing: " + listingId, exception.getMessage());
        verify(currentUserProvider).requireCurrentUser();
        verify(listingServiceClient).getListing(listingId);
    }

    @Test
    void shouldThrowExternalServiceExceptionWhenListingOwnerIdIsNull() {
        // given
        UUID listingId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .roles(Set.of("USER"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                null,
                "DRAFT"
        );

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when + then
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> verifier.requireOwnerOrAdmin(listingId)
        );

        assertTrue(exception.getMessage().contains("listing-service error"));
        assertTrue(exception.getMessage().contains("Listing ownership data missing for listing " + listingId));

        verify(currentUserProvider).requireCurrentUser();
        verify(listingServiceClient).getListing(listingId);
    }

    @Test
    void shouldAllowReadWhenListingIsPublished() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                UUID.randomUUID(),
                "PUBLISHED"
        );

        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when + then
        assertDoesNotThrow(() -> verifier.requireCanRead(listingId));

        verify(listingServiceClient).getListing(listingId);
        verify(currentUserProvider, never()).requireCurrentUser();
    }

    @Test
    void shouldAllowReadWhenListingIsPublishedIgnoringCase() {
        // given
        UUID listingId = UUID.randomUUID();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                UUID.randomUUID(),
                "published"
        );

        when(listingServiceClient.getListing(listingId)).thenReturn(listing);

        // when + then
        assertDoesNotThrow(() -> verifier.requireCanRead(listingId));

        verify(listingServiceClient).getListing(listingId);
        verify(currentUserProvider, never()).requireCurrentUser();
    }

    @Test
    void shouldAllowReadWhenRequesterIsOwnerAndListingIsNotPublished() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser ownerUser = CurrentUser.builder()
                .userId(ownerId)
                .email("owner@example.com")
                .roles(Set.of("USER"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(listingServiceClient.getListing(listingId)).thenReturn(listing);
        when(currentUserProvider.requireCurrentUser()).thenReturn(ownerUser);

        // when + then
        assertDoesNotThrow(() -> verifier.requireCanRead(listingId));

        verify(listingServiceClient).getListing(listingId);
        verify(currentUserProvider).requireCurrentUser();
    }

    @Test
    void shouldAllowReadWhenRequesterIsAdminAndListingIsNotPublished() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser adminUser = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("admin@example.com")
                .roles(Set.of("ADMIN"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(listingServiceClient.getListing(listingId)).thenReturn(listing);
        when(currentUserProvider.requireCurrentUser()).thenReturn(adminUser);

        // when + then
        assertDoesNotThrow(() -> verifier.requireCanRead(listingId));

        verify(listingServiceClient).getListing(listingId);
        verify(currentUserProvider).requireCurrentUser();
    }

    @Test
    void shouldThrowListingOwnershipExceptionWhenRequesterIsNeitherOwnerNorAdminAndListingIsNotPublished() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        CurrentUser stranger = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("stranger@example.com")
                .roles(Set.of("USER"))
                .build();

        ListingDetailsDto listing = new ListingDetailsDto(
                listingId,
                ownerId,
                "DRAFT"
        );

        when(listingServiceClient.getListing(listingId)).thenReturn(listing);
        when(currentUserProvider.requireCurrentUser()).thenReturn(stranger);

        // when + then
        ListingOwnershipException exception = assertThrows(
                ListingOwnershipException.class,
                () -> verifier.requireCanRead(listingId)
        );

        assertEquals("Access denied for listing: " + listingId, exception.getMessage());
        verify(listingServiceClient).getListing(listingId);
        verify(currentUserProvider).requireCurrentUser();
    }
}