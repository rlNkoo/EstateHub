package com.rlnkoo.mediaservice.domain.exception;

import java.util.UUID;

public class PhotoLimitExceededException extends RuntimeException {

  public PhotoLimitExceededException(UUID listingId, int max) {
    super("Photo limit exceeded for listing " + listingId + " (max=" + max + ")");
  }
}