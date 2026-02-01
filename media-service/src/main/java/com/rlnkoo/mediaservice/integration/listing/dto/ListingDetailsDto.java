package com.rlnkoo.mediaservice.integration.listing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingDetailsDto(
        UUID id,
        UUID ownerId,
        String status
) {}