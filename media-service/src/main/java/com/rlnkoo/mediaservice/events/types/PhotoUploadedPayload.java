package com.rlnkoo.mediaservice.events.types;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PhotoUploadedPayload(
        UUID mediaId,
        UUID listingId,
        UUID ownerId,

        String bucket,
        String objectKey,
        String thumbnailKey,

        String contentType,
        long sizeBytes,
        String sha256,

        Instant uploadedAt
) {}