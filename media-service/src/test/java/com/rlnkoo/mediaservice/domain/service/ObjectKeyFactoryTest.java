package com.rlnkoo.mediaservice.domain.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectKeyFactoryTest {

    private final ObjectKeyFactory objectKeyFactory = new ObjectKeyFactory();

    @Test
    void shouldCreatePhotoObjectKey() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        String extension = ".jpg";

        // when
        String result = objectKeyFactory.photoObjectKey(listingId, mediaId, extension);

        // then
        assertEquals(
                "listings/" + listingId + "/photos/" + mediaId + ".jpg",
                result
        );
    }

    @Test
    void shouldCreateThumbnailObjectKey() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        // when
        String result = objectKeyFactory.thumbnailObjectKey(listingId, mediaId);

        // then
        assertEquals(
                "listings/" + listingId + "/thumbs/" + mediaId + ".jpg",
                result
        );
    }

    @Test
    void shouldCreatePhotoObjectKeyWithEmptyExtension() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        // when
        String result = objectKeyFactory.photoObjectKey(listingId, mediaId, "");

        // then
        assertEquals(
                "listings/" + listingId + "/photos/" + mediaId,
                result
        );
    }
}