package com.rlnkoo.searchservice.api.search.dto;

import lombok.Builder;

@Builder
public record SearchFacetBucketResponse(
        String value,
        long count
) {
}