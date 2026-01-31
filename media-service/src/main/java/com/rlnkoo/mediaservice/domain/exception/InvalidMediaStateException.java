package com.rlnkoo.mediaservice.domain.exception;

import com.rlnkoo.mediaservice.domain.model.MediaStatus;

import java.util.UUID;

public class InvalidMediaStateException extends RuntimeException {

    public InvalidMediaStateException(UUID mediaId, MediaStatus status, String operation) {
        super("Invalid media state for operation=" + operation + " mediaId=" + mediaId + " status=" + status);
    }
}