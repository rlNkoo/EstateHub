package com.rlnkoo.searchservice.api.search.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SearchListingsResponse(
        List<SearchListingItemResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size,
        String sort
) {
}