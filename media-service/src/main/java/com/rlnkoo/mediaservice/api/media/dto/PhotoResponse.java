package com.rlnkoo.mediaservice.api.media.dto;

import lombok.Builder;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PhotoResponse(
        UUID mediaId,
        UUID listingId,
        String contentType,
        long sizeBytes,
        String sha256,
        Instant createdAt,
        URL url,
        URL thumbnailUrl
) {}