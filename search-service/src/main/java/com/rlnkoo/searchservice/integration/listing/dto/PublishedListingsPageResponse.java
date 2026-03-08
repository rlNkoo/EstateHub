package com.rlnkoo.searchservice.integration.listing.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PublishedListingsPageResponse(
        List<PublishedListingForReindexResponse> items,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}