package com.rlnkoo.searchservice.api.search.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record SearchListingItemResponse(
        UUID id,
        String title,
        String description,
        BigDecimal priceAmount,
        String currencyCode,
        String country,
        String city,
        String street,
        String postalCode,
        BigDecimal area,
        Integer rooms,
        Integer floor,
        String propertyType,
        List<UUID> photoIds,
        Instant publishedAt
) {
}