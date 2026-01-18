package com.rlnkoo.listingservice.persistence.entity;

import com.rlnkoo.listingservice.domain.model.PropertyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "listing_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_listing_version", columnNames = {"listing_id", "version_no"})
        },
        indexes = {
                @Index(name = "ix_listing_versions_listing_id", columnList = "listing_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingVersionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    @Column(name = "version_no", nullable = false, updatable = false)
    private int versionNo;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "price_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Embedded
    private AddressEmbeddable address;

    @Column(name = "area", nullable = false, precision = 19, scale = 2)
    private BigDecimal area;

    @Column(name = "rooms")
    private Integer rooms;

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @ElementCollection
    @CollectionTable(
            name = "listing_version_photos",
            joinColumns = @JoinColumn(name = "listing_version_id", nullable = false)
    )
    @Column(name = "media_id", nullable = false, updatable = false)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<UUID> photoIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    @Getter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressEmbeddable {

        @Column(name = "country", nullable = false, length = 80)
        private String country;

        @Column(name = "city", nullable = false, length = 120)
        private String city;

        @Column(name = "street", length = 200)
        private String street;

        @Column(name = "postal_code", length = 20)
        private String postalCode;
    }
}