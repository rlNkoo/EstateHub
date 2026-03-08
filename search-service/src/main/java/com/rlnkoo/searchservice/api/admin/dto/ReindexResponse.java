package com.rlnkoo.searchservice.api.admin.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ReindexResponse(
        boolean started,
        String message,
        Instant timestamp
) {
}