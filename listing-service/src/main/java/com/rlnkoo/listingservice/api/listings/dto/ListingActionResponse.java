package com.rlnkoo.listingservice.api.listings.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ListingActionResponse(
        UUID id,
        String status,
        int version
) {}