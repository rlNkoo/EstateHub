package com.rlnkoo.mediaservice.domain.model;

import java.util.Arrays;

public enum MediaType {

    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    private final String contentType;

    MediaType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static MediaType fromContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content-Type is required");
        }

        return Arrays.stream(values())
                .filter(type -> type.contentType.equalsIgnoreCase(contentType))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unsupported media type: " + contentType)
                );
    }
}