package com.rlnkoo.listingservice.domain.exception;

import java.util.UUID;

public class ListingNotPublishableException extends RuntimeException {

    public ListingNotPublishableException(UUID listingId, String reason) {
        super("Listing cannot be published (id=" + listingId + "): " + reason);
    }
}