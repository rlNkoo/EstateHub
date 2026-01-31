package com.rlnkoo.mediaservice.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "media_variants",
        indexes = {
                @Index(name = "ix_media_variants_media_id", columnList = "media_id"),
                @Index(name = "ix_media_variants_type", columnList = "variant_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_media_variants_media_type", columnNames = {"media_id", "variant_type"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaVariantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;

    @Column(name = "variant_type", nullable = false, length = 50, updatable = false)
    private String variantType;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}