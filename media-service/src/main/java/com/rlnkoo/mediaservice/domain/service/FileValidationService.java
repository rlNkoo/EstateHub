package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.domain.exception.FileTooLargeException;
import com.rlnkoo.mediaservice.domain.exception.InvalidContentTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileValidationService {

    private final MediaProperties mediaProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        long size = file.getSize();
        long max = mediaProperties.getMaxFileSizeBytes();
        if (size > max) {
            throw new FileTooLargeException(size, max);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidContentTypeException("null");
        }

        boolean allowed = mediaProperties.getAllowedContentTypes().stream()
                .anyMatch(allowedType -> allowedType.equalsIgnoreCase(contentType));

        if (!allowed) {
            throw new InvalidContentTypeException(contentType);
        }
    }
}