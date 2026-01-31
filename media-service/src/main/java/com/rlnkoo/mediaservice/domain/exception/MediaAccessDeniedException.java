package com.rlnkoo.mediaservice.domain.exception;

import java.util.UUID;

public class MediaAccessDeniedException extends RuntimeException {

    public MediaAccessDeniedException(UUID mediaId) {
        super("Access denied to media: " + mediaId);
    }
}