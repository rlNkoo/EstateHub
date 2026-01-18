package com.rlnkoo.listingservice.domain.exception;

import java.util.UUID;

public class ListingOwnershipException extends RuntimeException {

    public ListingOwnershipException(UUID listingId) {
        super("Access denied for listing: " + listingId);
    }
}