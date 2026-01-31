package com.rlnkoo.mediaservice.domain.service;

import java.util.UUID;

public interface ListingOwnershipVerifier {

    UUID requireOwnerOrAdmin(UUID listingId);
}