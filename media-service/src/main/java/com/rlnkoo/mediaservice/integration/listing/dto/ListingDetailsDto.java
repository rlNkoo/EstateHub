package com.rlnkoo.mediaservice.integration.listing.dto;

import java.util.UUID;

public record ListingDetailsDto(
        UUID id,
        UUID ownerId,
        String status
) {}