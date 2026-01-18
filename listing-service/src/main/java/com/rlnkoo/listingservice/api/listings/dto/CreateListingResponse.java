package com.rlnkoo.listingservice.api.listings.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateListingResponse(
        UUID id
) {
}