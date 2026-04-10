package com.rlnkoo.mediaservice.domain.exception;

public class InvalidContentTypeException extends RuntimeException {

    public InvalidContentTypeException(String contentType) {
        super("Invalid content type: " + contentType);
    }
}