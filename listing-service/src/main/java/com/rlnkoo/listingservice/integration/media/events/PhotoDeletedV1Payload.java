package com.rlnkoo.listingservice.integration.media.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoDeletedV1Payload(
        UUID mediaId,
        UUID listingId
) {}