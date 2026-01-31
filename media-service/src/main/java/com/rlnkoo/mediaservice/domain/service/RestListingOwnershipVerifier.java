package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.ListingOwnershipException;
import com.rlnkoo.mediaservice.integration.listing.ListingServiceClient;
import com.rlnkoo.mediaservice.integration.listing.dto.ListingDetailsDto;
import com.rlnkoo.mediaservice.security.CurrentUser;
import com.rlnkoo.mediaservice.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestListingOwnershipVerifier implements ListingOwnershipVerifier {

    private final ListingServiceClient listingServiceClient;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public UUID requireOwnerOrAdmin(UUID listingId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();

        if (user.roles().contains("ADMIN")) {
            ListingDetailsDto listing = listingServiceClient.getListing(listingId);
            return listing.ownerId();
        }

        ListingDetailsDto listing = listingServiceClient.getListing(listingId);

        boolean isOwner = user.userId().equals(listing.ownerId());
        if (!isOwner) {
            log.warn("Access denied for listingId=[{}] ownerId=[{}] requesterId=[{}] roles=[{}]",
                    listingId, listing.ownerId(), user.userId(), user.roles());
            throw new ListingOwnershipException(listingId);
        }

        return listing.ownerId();
    }

    @Override
    public void requireCanRead(UUID listingId) {
        var listing = listingServiceClient.getListing(listingId);

        if ("PUBLISHED".equalsIgnoreCase(listing.status())) {
            return;
        }

        CurrentUser user = currentUserProvider.requireCurrentUser();

        boolean isAdmin = user.roles().contains("ADMIN");
        boolean isOwner = user.userId().equals(listing.ownerId());

        if (!isAdmin && !isOwner) {
            throw new ListingOwnershipException(listingId);
        }
    }
}