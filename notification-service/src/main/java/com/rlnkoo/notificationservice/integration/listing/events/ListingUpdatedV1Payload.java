package com.rlnkoo.notificationservice.integration.listing.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingUpdatedV1Payload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,
        String title
) {}