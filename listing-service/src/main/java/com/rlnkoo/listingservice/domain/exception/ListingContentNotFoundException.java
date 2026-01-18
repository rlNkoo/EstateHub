package com.rlnkoo.listingservice.domain.exception;

import java.util.UUID;

public class ListingContentNotFoundException extends RuntimeException {

    public ListingContentNotFoundException(UUID listingId) {
        super("Listing content not found for listing: " + listingId);
    }
}