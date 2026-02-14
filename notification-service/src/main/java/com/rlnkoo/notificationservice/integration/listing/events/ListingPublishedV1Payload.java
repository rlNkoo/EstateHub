package com.rlnkoo.notificationservice.integration.listing.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingPublishedV1Payload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,
        Instant publishedAt,
        String title
) {}