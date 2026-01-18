package com.rlnkoo.listingservice.domain.exception;

import com.rlnkoo.listingservice.domain.model.ListingStatus;

import java.util.UUID;

public class ListingNotEditableException extends RuntimeException {

    public ListingNotEditableException(UUID listingId, ListingStatus status) {
        super("Listing is not editable in status " + status + " (id=" + listingId + ")");
    }
}