package com.rlnkoo.mediaservice.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "media_publications",
        indexes = {
                @Index(name = "ix_media_publications_media_id", columnList = "media_id"),
                @Index(name = "ix_media_publications_listing_id", columnList = "listing_id"),
                @Index(name = "ix_media_publications_public_visible", columnList = "public_visible")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_media_publications_listing_media",
                        columnNames = {"listing_id", "media_id"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPublicationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}