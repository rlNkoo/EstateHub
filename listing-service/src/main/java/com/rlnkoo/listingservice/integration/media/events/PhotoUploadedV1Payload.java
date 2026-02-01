package com.rlnkoo.listingservice.integration.media.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoUploadedV1Payload(
        UUID mediaId,
        UUID listingId
) {}