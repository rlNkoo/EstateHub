package com.rlnkoo.listingservice.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Listing {

    private final UUID id;
    private final UUID ownerId;

    private ListingStatus status;
    private int currentVersion;

    private String title;
    private String description;
    private Money price;
    private Address address;
    private BigDecimal area;
    private Integer rooms;
    private Integer floor;
    private PropertyType propertyType;
    private List<String> photoRefs;

    private Listing(UUID id, UUID ownerId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        this.status = ListingStatus.DRAFT;
        this.currentVersion = 1;
        this.photoRefs = List.of();
    }

    public static Listing create(UUID id, UUID ownerId) {
        return new Listing(id, ownerId);
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public ListingStatus status() {
        return status;
    }

    public int currentVersion() {
        return currentVersion;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Money price() {
        return price;
    }

    public Address address() {
        return address;
    }

    public BigDecimal area() {
        return area;
    }

    public Integer rooms() {
        return rooms;
    }

    public Integer floor() {
        return floor;
    }

    public PropertyType propertyType() {
        return propertyType;
    }

    public List<String> photoRefs() {
        return photoRefs;
    }

    public void updateDraft(
            String title,
            String description,
            Money price,
            Address address,
            BigDecimal area,
            Integer rooms,
            Integer floor,
            PropertyType propertyType,
            List<String> photoRefs
    ) {
        if (!status.isEditable()) {
            throw new IllegalStateException("Listing is not editable in status: " + status);
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (title.length() > 120) {
            throw new IllegalArgumentException("Title must not be longer than 120 characters");
        }
        if (description != null && description.length() > 5000) {
            throw new IllegalArgumentException("Description must not be longer than 5000 characters");
        }
        Objects.requireNonNull(price, "Price must not be null");
        Objects.requireNonNull(address, "Address must not be null");
        Objects.requireNonNull(area, "Area must not be null");
        if (area.signum() <= 0) {
            throw new IllegalArgumentException("Area must be greater than 0");
        }
        if (rooms != null && (rooms < 1 || rooms > 50)) {
            throw new IllegalArgumentException("Rooms must be between 1 and 50");
        }
        if (floor != null && floor < -10) {
            throw new IllegalArgumentException("Floor must be greater than or equal to -10");
        }
        Objects.requireNonNull(propertyType, "PropertyType must not be null");

        this.title = title;
        this.description = description;
        this.price = price;
        this.address = address;
        this.area = area;
        this.rooms = rooms;
        this.floor = floor;
        this.propertyType = propertyType;
        this.photoRefs = (photoRefs == null) ? List.of() : List.copyOf(photoRefs);
        this.currentVersion += 1;
    }

    public void publish() {
        if (!status.canPublish()) {
            throw new IllegalStateException("Listing cannot be published from status: " + status);
        }
        ensurePublishable();
        this.status = ListingStatus.PUBLISHED;
    }

    public void archive() {
        if (!status.canArchive()) {
            throw new IllegalStateException("Listing cannot be archived from status: " + status);
        }
        this.status = ListingStatus.ARCHIVED;
    }

    private void ensurePublishable() {
        if (title == null || title.isBlank()) {
            throw new IllegalStateException("Listing cannot be published: title missing");
        }
        if (price == null) {
            throw new IllegalStateException("Listing cannot be published: price missing");
        }
        if (address == null) {
            throw new IllegalStateException("Listing cannot be published: address missing");
        }
        if (area == null || area.signum() <= 0) {
            throw new IllegalStateException("Listing cannot be published: area invalid");
        }
        if (propertyType == null) {
            throw new IllegalStateException("Listing cannot be published: propertyType missing");
        }
    }
}