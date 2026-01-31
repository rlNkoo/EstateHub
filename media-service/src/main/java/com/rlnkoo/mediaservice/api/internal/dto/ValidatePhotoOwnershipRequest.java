package com.rlnkoo.mediaservice.api.internal.dto;

import lombok.Builder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Builder
public record ValidatePhotoOwnershipRequest(
        UUID requesterId,
        Set<String> requesterRoles,
        List<UUID> photoIds
) {
}