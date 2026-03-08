package com.rlnkoo.searchservice.api.admin.dto;

import lombok.Builder;

@Builder
public record IndexInfoResponse(
        String indexName,
        boolean exists,
        long documentCount
) {
}