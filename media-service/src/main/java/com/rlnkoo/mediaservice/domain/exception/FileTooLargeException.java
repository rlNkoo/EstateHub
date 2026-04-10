package com.rlnkoo.mediaservice.domain.exception;

public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(long sizeBytes, long maxBytes) {
        super("File too large: " + sizeBytes + " bytes (max=" + maxBytes + ")");
    }
}