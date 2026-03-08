package com.rlnkoo.searchservice.api.search.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record SearchListingsRequest(
        String q,
        String city,
        String country,
        String propertyType,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        BigDecimal areaFrom,
        BigDecimal areaTo,
        Integer rooms,
        Integer floor,
        Integer page,
        Integer size,
        String sort
) {
}