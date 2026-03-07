package com.rlnkoo.searchservice.integration.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingUpdatedV1Payload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,
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