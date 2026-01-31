package com.rlnkoo.mediaservice.domain.exception;

public class AuthenticationRequiredException extends RuntimeException {

  public AuthenticationRequiredException() {
    super("Authentication required");
  }
}