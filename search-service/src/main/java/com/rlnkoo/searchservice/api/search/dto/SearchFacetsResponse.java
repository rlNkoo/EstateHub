package com.rlnkoo.searchservice.api.search.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record SearchFacetsResponse(
        List<SearchFacetBucketResponse> cities,
        List<SearchFacetBucketResponse> propertyTypes,
        List<SearchFacetBucketResponse> rooms
) {
}