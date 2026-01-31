package com.rlnkoo.mediaservice.api.media.dto;

import lombok.Builder;

import java.net.URL;
import java.util.UUID;

@Builder
public record UploadPhotoResponse(
        UUID mediaId,
        UUID listingId,
        URL url,
        URL thumbnailUrl
) {}