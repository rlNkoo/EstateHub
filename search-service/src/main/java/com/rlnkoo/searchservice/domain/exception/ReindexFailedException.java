package com.rlnkoo.searchservice.domain.exception;

public class ReindexFailedException extends RuntimeException {

    public ReindexFailedException(String message) {
        super(message);
    }

    public ReindexFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}