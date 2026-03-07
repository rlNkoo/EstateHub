package com.rlnkoo.searchservice.integration.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingPublishedV1Payload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,
        Instant publishedAt,
        String title,
        String description,
        BigDecimal priceAmount,
        String currencyCode,
        ListingEventAddressPayload address,
        BigDecimal area,
        Integer rooms,
        Integer floor,
        String propertyType,
        List<UUID> photoIds
) {
}