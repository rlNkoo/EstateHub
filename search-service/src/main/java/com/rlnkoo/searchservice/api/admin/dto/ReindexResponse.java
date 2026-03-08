package com.rlnkoo.searchservice.api.admin.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ReindexResponse(
        boolean completed,
        String message,
        int fetchedCount,
        int indexedCount,
        int failedCount,
        int processedPages,
        Instant timestamp
) {
}