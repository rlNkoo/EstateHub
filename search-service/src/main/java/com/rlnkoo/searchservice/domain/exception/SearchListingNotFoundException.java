package com.rlnkoo.searchservice.domain.exception;

import java.util.UUID;

public class SearchListingNotFoundException extends RuntimeException {

    public SearchListingNotFoundException(UUID listingId) {
        super("Listing not found: " + listingId);
    }
}