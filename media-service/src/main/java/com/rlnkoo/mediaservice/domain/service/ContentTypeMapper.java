package com.rlnkoo.mediaservice.domain.service;

import org.springframework.stereotype.Component;

@Component
public class ContentTypeMapper {

    public String extensionFor(String contentType) {
        if (contentType == null) return "";

        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}