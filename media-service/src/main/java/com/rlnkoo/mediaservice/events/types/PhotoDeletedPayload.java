package com.rlnkoo.mediaservice.events.types;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PhotoDeletedPayload(
        UUID mediaId,
        UUID ownerId,
        Instant deletedAt
) {
}