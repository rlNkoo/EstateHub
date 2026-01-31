package com.rlnkoo.mediaservice.domain.exception;

import java.util.UUID;

public class MediaOwnershipException extends RuntimeException {
    public MediaOwnershipException(UUID mediaId) {
        super("Access denied for media: " + mediaId);
    }
}