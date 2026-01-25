package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.api.listings.dto.UpdateListingRequest;
import com.rlnkoo.listingservice.domain.exception.*;
import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.domain.model.PropertyType;
import com.rlnkoo.listingservice.events.producer.ListingEventsPublisher;
import com.rlnkoo.listingservice.events.types.ListingArchivedPayload;
import com.rlnkoo.listingservice.events.types.ListingPublishedPayload;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import com.rlnkoo.listingservice.security.CurrentUser;
import com.rlnkoo.listingservice.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingVersionRepository versionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ListingEventsPublisher eventsPublisher;

    @Transactional
    public UUID createDraft() {
        CurrentUser user = currentUserProvider.requireCurrentUser();

        UUID listingId = UUID.randomUUID();
        log.info("Create draft request ownerId=[{}] listingId=[{}]", user.userId(), listingId);

        ListingEntity listing = ListingEntity.builder()
                .id(listingId)
                .ownerId(user.userId())
                .status(ListingStatus.DRAFT)
                .currentVersion(1)
                .publishedVersion(null)
                .build();

        listingRepository.save(listing);

        log.info("Draft created ownerId=[{}] listingId=[{}] status=[{}] currentVersion=[{}]",
                user.userId(), listingId, listing.getStatus(), listing.getCurrentVersion());

        return listingId;
    }

    @Transactional
    public ListingEntity update(UUID listingId, UpdateListingRequest request) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("Update request listingId=[{}] requesterId=[{}]", listingId, user.userId());

        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Update failed: listing not found listingId=[{}] requesterId=[{}]",
                            listingId, user.userId());
                    return new ListingNotFoundException(listingId);
                });

        assertOwnerOrAdmin(user, listing);

        if (listing.getStatus() == ListingStatus.ARCHIVED) {
            log.warn("Update failed: archived listing listingId=[{}] requesterId=[{}]",
                    listingId, user.userId());
            throw new ListingNotEditableException(listingId, listing.getStatus());
        }

        PropertyType propertyType;
        try {
            propertyType = PropertyType.valueOf(request.propertyType());
        } catch (IllegalArgumentException ex) {
            log.warn("Update failed: invalid propertyType=[{}] listingId=[{}] requesterId=[{}]",
                    request.propertyType(), listingId, user.userId());
            throw new InvalidPropertyTypeException(request.propertyType());
        }

        int newVersion = listing.getCurrentVersion() + 1;

        ListingVersionEntity versionEntity = ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(newVersion)
                .title(request.title())
                .description(request.description())
                .priceAmount(request.priceAmount())
                .currencyCode(request.currencyCode())
                .address(ListingVersionEntity.AddressEmbeddable.builder()
                        .country(request.address().country())
                        .city(request.address().city())
                        .street(request.address().street())
                        .postalCode(request.address().postalCode())
                        .build())
                .area(request.area())
                .rooms(request.rooms())
                .floor(request.floor())
                .propertyType(propertyType)
                .photoIds(request.photoIds() == null ? List.of() : List.copyOf(request.photoIds()))
                .build();

        versionRepository.save(versionEntity);

        listing.setCurrentVersion(newVersion);

        boolean liveUpdate = listing.getStatus() == ListingStatus.PUBLISHED;

        if (liveUpdate) {
            ensurePublishable(listingId, versionEntity);
            listing.setPublishedVersion(newVersion);
        }

        listingRepository.save(listing);

        if (liveUpdate) {
            eventsPublisher.publishListingUpdated(
                    listingId,
                    ListingUpdatedPayload.builder()
                            .listingId(listingId)
                            .ownerId(listing.getOwnerId())
                            .status(listing.getStatus().name())
                            .version(listing.getPublishedVersion())
                            .title(versionEntity.getTitle())
                            .description(versionEntity.getDescription())
                            .priceAmount(versionEntity.getPriceAmount())
                            .currencyCode(versionEntity.getCurrencyCode())
                            .address(ListingUpdatedPayload.AddressPayload.builder()
                                    .country(versionEntity.getAddress().getCountry())
                                    .city(versionEntity.getAddress().getCity())
                                    .street(versionEntity.getAddress().getStreet())
                                    .postalCode(versionEntity.getAddress().getPostalCode())
                                    .build())
                            .area(versionEntity.getArea())
                            .rooms(versionEntity.getRooms())
                            .floor(versionEntity.getFloor())
                            .propertyType(versionEntity.getPropertyType().name())
                            .photoIds(versionEntity.getPhotoIds())
                            .build()
            );

            log.info("Listing updated LIVE (event published) listingId=[{}] requesterId=[{}] newPublishedVersion=[{}]",
                    listingId, user.userId(), listing.getPublishedVersion());
        } else {
            log.info("Listing updated (draft/no event) listingId=[{}] requesterId=[{}] newCurrentVersion=[{}] status=[{}]",
                    listingId, user.userId(), newVersion, listing.getStatus());
        }
        return listing;
    }

    @Transactional
    public ListingEntity publish(UUID listingId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("Publish request listingId=[{}] requesterId=[{}]", listingId, user.userId());

        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Publish failed: listing not found listingId=[{}] requesterId=[{}]",
                            listingId, user.userId());
                    return new ListingNotFoundException(listingId);
                });

        assertOwnerOrAdmin(user, listing);

        if (!listing.getStatus().canPublish()) {
            log.warn("Publish failed: invalid status transition listingId=[{}] from=[{}] to=[PUBLISHED] requesterId=[{}]",
                    listingId, listing.getStatus(), user.userId());
            throw new InvalidListingStatusTransitionException(listingId, listing.getStatus(), ListingStatus.PUBLISHED);
        }

        ListingVersionEntity current = versionRepository
                .findByListingIdAndVersionNo(listingId, listing.getCurrentVersion())
                .orElseThrow(() -> {
                    log.warn("Publish failed: content not found listingId=[{}] version=[{}] requesterId=[{}]",
                            listingId, listing.getCurrentVersion(), user.userId());
                    return new ListingContentNotFoundException(listingId);
                });

        ensurePublishable(listingId, current);

        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedVersion(listing.getCurrentVersion());
        listingRepository.save(listing);

        Instant now = Instant.now();

        eventsPublisher.publishListingPublished(
                listingId,
                ListingPublishedPayload.builder()
                        .listingId(listingId)
                        .ownerId(listing.getOwnerId())
                        .status(listing.getStatus().name())
                        .version(listing.getPublishedVersion())
                        .publishedAt(now)
                        .title(current.getTitle())
                        .description(current.getDescription())
                        .priceAmount(current.getPriceAmount())
                        .currencyCode(current.getCurrencyCode())
                        .address(ListingUpdatedPayload.AddressPayload.builder()
                                .country(current.getAddress().getCountry())
                                .city(current.getAddress().getCity())
                                .street(current.getAddress().getStreet())
                                .postalCode(current.getAddress().getPostalCode())
                                .build())
                        .area(current.getArea())
                        .rooms(current.getRooms())
                        .floor(current.getFloor())
                        .propertyType(current.getPropertyType().name())
                        .photoIds(current.getPhotoIds())
                        .build()
        );

        log.info("Listing published listingId=[{}] requesterId=[{}] publishedVersion=[{}]",
                listingId, user.userId(), listing.getPublishedVersion());

        return listing;
    }

    @Transactional
    public ListingEntity archive(UUID listingId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("Archive request listingId=[{}] requesterId=[{}]", listingId, user.userId());

        ListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Archive failed: listing not found listingId=[{}] requesterId=[{}]",
                            listingId, user.userId());
                    return new ListingNotFoundException(listingId);
                });

        assertOwnerOrAdmin(user, listing);

        if (!listing.getStatus().canArchive()) {
            log.warn("Archive failed: invalid status transition listingId=[{}] from=[{}] to=[ARCHIVED] requesterId=[{}]",
                    listingId, listing.getStatus(), user.userId());
            throw new InvalidListingStatusTransitionException(listingId, listing.getStatus(), ListingStatus.ARCHIVED);
        }

        listing.setStatus(ListingStatus.ARCHIVED);
        listingRepository.save(listing);

        int archivedVersion = listing.getPublishedVersion() != null
                ? listing.getPublishedVersion()
                : listing.getCurrentVersion();

        eventsPublisher.publishListingArchived(
                listingId,
                ListingArchivedPayload.builder()
                        .listingId(listingId)
                        .ownerId(listing.getOwnerId())
                        .status(listing.getStatus().name())
                        .version(archivedVersion)
                        .archivedAt(Instant.now())
                        .build()
        );

        log.info("Listing archived listingId=[{}] requesterId=[{}] archivedVersion=[{}]",
                listingId, user.userId(), archivedVersion);

        return listing;
    }

    @Transactional(readOnly = true)
    public Optional<ListingEntity> getListing(UUID listingId) {
        return listingRepository.findById(listingId);
    }

    @Transactional(readOnly = true)
    public Optional<ListingVersionEntity> getListingVersion(UUID listingId, int versionNo) {
        return versionRepository.findByListingIdAndVersionNo(listingId, versionNo);
    }

    @Transactional(readOnly = true)
    public int resolveVersionForRead(ListingEntity listing) {
        if (listing.isPublished() && listing.getPublishedVersion() != null) {
            return listing.getPublishedVersion();
        }
        return listing.getCurrentVersion();
    }

    @Transactional(readOnly = true)
    public List<ListingEntity> getMyListings() {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("My listings request requesterId=[{}]", user.userId());
        return listingRepository.findAllByOwnerIdOrderByUpdatedAtDesc(user.userId());
    }

    private void assertOwnerOrAdmin(CurrentUser user, ListingEntity listing) {
        boolean isOwner = user.userId().equals(listing.getOwnerId());
        boolean isAdmin = user.roles().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            log.warn("Access denied listingId=[{}] ownerId=[{}] requesterId=[{}] roles=[{}]",
                    listing.getId(), listing.getOwnerId(), user.userId(), user.roles());
            throw new ListingOwnershipException(listing.getId());
        }
    }

    private void ensurePublishable(UUID listingId, ListingVersionEntity current) {
        if (current.getTitle() == null || current.getTitle().isBlank()) {
            log.warn("Publish failed: title missing listingId=[{}]", listingId);
            throw new ListingNotPublishableException(listingId, "title missing");
        }
        if (current.getPriceAmount() == null) {
            log.warn("Publish failed: price missing listingId=[{}]", listingId);
            throw new ListingNotPublishableException(listingId, "price missing");
        }
        if (current.getCurrencyCode() == null || current.getCurrencyCode().isBlank()) {
            log.warn("Publish failed: currency missing listingId=[{}]", listingId);
            throw new ListingNotPublishableException(listingId, "currency missing");
        }
        if (current.getAddress() == null
                || current.getAddress().getCountry() == null || current.getAddress().getCountry().isBlank()
                || current.getAddress().getCity() == null || current.getAddress().getCity().isBlank()) {
            log.warn("Publish failed: address missing listingId=[{}]", listingId);
            throw new ListingNotPublishableException(listingId, "address missing");
        }
        if (current.getArea() == null || current.getArea().signum() <= 0) {
            log.warn("Publish failed: area invalid listingId=[{}] area=[{}]", listingId, current.getArea());
            throw new ListingNotPublishableException(listingId, "area invalid");
        }
        if (current.getPropertyType() == null) {
            log.warn("Publish failed: propertyType missing listingId=[{}]", listingId);
            throw new ListingNotPublishableException(listingId, "propertyType missing");
        }
    }
}