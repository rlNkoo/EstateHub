package com.rlnkoo.listingservice.domain.model;

public enum ListingStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED;

    public boolean canPublish() {
        return this == DRAFT;
    }

    public boolean canArchive() {
        return this == PUBLISHED;
    }

    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }
}