package com.rlnkoo.listingservice.api.admin.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record PublishedListingForReindexResponse(
        UUID id,
        UUID ownerId,
        String status,
        int version,
        Instant publishedAt,
        Instant updatedAt,
        String title,
        String description,
        BigDecimal priceAmount,
        String currencyCode,
        AddressResponse address,
        BigDecimal area,
        Integer rooms,
        Integer floor,
        String propertyType,
        List<UUID> photoIds
) {

    @Builder
    public record AddressResponse(
            String country,
            String city,
            String street,
            String postalCode
    ) {
    }
}