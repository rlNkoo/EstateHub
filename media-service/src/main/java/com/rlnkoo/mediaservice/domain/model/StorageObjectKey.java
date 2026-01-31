package com.rlnkoo.mediaservice.domain.model;

import java.util.UUID;

public final class StorageObjectKey {

    private static final String ROOT = "photos";

    private StorageObjectKey() {
    }

    public static String original(UUID ownerId, UUID mediaId) {
        return ROOT + "/" + ownerId + "/" + mediaId + "/original";
    }

    public static String thumbnail(UUID ownerId, UUID mediaId, int size) {
        return ROOT + "/" + ownerId + "/" + mediaId + "/thumb_" + size;
    }
}