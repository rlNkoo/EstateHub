package com.rlnkoo.listingservice.api.listings.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record ListingDetailsResponse(
        UUID id,
        UUID ownerId,
        String status,
        int version,

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