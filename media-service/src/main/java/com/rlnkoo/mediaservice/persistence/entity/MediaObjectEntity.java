package com.rlnkoo.mediaservice.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "media_objects",
        indexes = {
                @Index(name = "ix_media_objects_listing_id", columnList = "listing_id"),
                @Index(name = "ix_media_objects_listing_active", columnList = "listing_id, deleted_at"),
                @Index(name = "ix_media_objects_owner_id", columnList = "owner_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaObjectEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "bucket", nullable = false, length = 120, updatable = false)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 500, updatable = false)
    private String objectKey;

    @Column(name = "thumbnail_key", length = 500, updatable = false)
    private String thumbnailKey;

    @Column(name = "content_type", nullable = false, length = 120, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "sha256", nullable = false, length = 64, updatable = false)
    private String sha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }
}