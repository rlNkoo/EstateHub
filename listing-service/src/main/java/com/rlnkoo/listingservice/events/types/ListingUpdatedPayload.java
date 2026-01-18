package com.rlnkoo.listingservice.events.types;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record ListingUpdatedPayload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,

        String title,
        String description,

        BigDecimal priceAmount,
        String currencyCode,

        AddressPayload address,

        BigDecimal area,
        Integer rooms,
        Integer floor,
        String propertyType,

        List<UUID> photoIds
) {

    @Builder
    public record AddressPayload(
            String country,
            String city,
            String street,
            String postalCode
    ) {
    }
}