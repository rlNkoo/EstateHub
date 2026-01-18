package com.rlnkoo.listingservice.domain.exception;

import java.util.UUID;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(UUID listingId) {
        super("Listing not found: " + listingId);
    }
}