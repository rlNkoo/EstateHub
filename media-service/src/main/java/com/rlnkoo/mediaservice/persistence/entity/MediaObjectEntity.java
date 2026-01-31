package com.rlnkoo.mediaservice.persistence.entity;

import com.rlnkoo.mediaservice.domain.model.MediaStatus;
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
                @Index(name = "ix_media_objects_owner_id", columnList = "owner_id"),
                @Index(name = "ix_media_objects_status", columnList = "status"),
                @Index(name = "ix_media_objects_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaObjectEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = MediaStatus.UPLOADING;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return status == MediaStatus.DELETED || deletedAt != null;
    }
}