package com.rlnkoo.mediaservice.domain.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ObjectKeyFactory {

    public String photoObjectKey(UUID listingId, UUID mediaId, String extension) {
        return "listings/" + listingId + "/photos/" + mediaId + extension;
    }

    public String thumbnailObjectKey(UUID listingId, UUID mediaId) {
        return "listings/" + listingId + "/thumbs/" + mediaId + ".jpg";
    }
}