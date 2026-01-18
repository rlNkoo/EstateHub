package com.rlnkoo.listingservice.domain.exception;

import com.rlnkoo.listingservice.domain.model.ListingStatus;

import java.util.UUID;

public class InvalidListingStatusTransitionException extends RuntimeException {

    public InvalidListingStatusTransitionException(
            UUID listingId,
            ListingStatus from,
            ListingStatus to
    ) {
        super("Invalid listing status transition for " + listingId + ": " + from + " -> " + to);
    }
}