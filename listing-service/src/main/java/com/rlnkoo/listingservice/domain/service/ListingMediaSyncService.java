package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.events.producer.ListingEventsPublisher;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingMediaSyncService {

    private final ListingRepository listingRepository;
    private final ListingVersionRepository versionRepository;
    private final ListingEventsPublisher eventsPublisher;

    @Transactional
    public void onPhotoUploaded(UUID listingId, UUID mediaId) {
        applyPhotoChange(listingId, mediaId, true);
    }

    @Transactional
    public void onPhotoDeleted(UUID listingId, UUID mediaId) {
        applyPhotoChange(listingId, mediaId, false);
    }

    private void applyPhotoChange(UUID listingId, UUID mediaId, boolean add) {
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseGet(() -> {
                    log.warn("PhotoChange ignored: listing not found listingId=[{}] mediaId=[{}] add=[{}]",
                            listingId, mediaId, add);
                    return null;
                });
        if (listing == null) return;

        ListingVersionEntity base = loadVersionToRead(listing);

        int newVersionNo = listing.getCurrentVersion() + 1;

        List<UUID> newPhotoIds = base.getPhotoIds() == null
                ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(base.getPhotoIds());

        int before = newPhotoIds.size();
        if (add) {
            if (!newPhotoIds.contains(mediaId)) newPhotoIds.add(mediaId);
        } else {
            newPhotoIds.remove(mediaId);
        }
        int after = newPhotoIds.size();

        if (before == after) {
            log.info("PhotoChange no-op listingId=[{}] baseVersion=[{}] mediaId=[{}] add=[{}]",
                    listingId, base.getVersionNo(), mediaId, add);
            return;
        }

        ListingVersionEntity next = ListingVersionEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .versionNo(newVersionNo)
                .title(base.getTitle())
                .description(base.getDescription())
                .priceAmount(base.getPriceAmount())
                .currencyCode(base.getCurrencyCode())
                .address(base.getAddress())
                .area(base.getArea())
                .rooms(base.getRooms())
                .floor(base.getFloor())
                .propertyType(base.getPropertyType())
                .photoIds(List.copyOf(newPhotoIds))
                .build();

        versionRepository.save(next);
        listing.setCurrentVersion(newVersionNo);

        boolean liveUpdate = listing.getStatus() == ListingStatus.PUBLISHED;
        if (liveUpdate) {
            listing.setPublishedVersion(newVersionNo);
        }

        listingRepository.save(listing);

        log.info("PhotoChange applied listingId=[{}] {} mediaId=[{}] {}->{} newVersion=[{}] live=[{}]",
                listingId, add ? "ADD" : "REMOVE", mediaId, before, after, newVersionNo, liveUpdate);

        if (liveUpdate) {
            eventsPublisher.publishListingUpdated(listingId, toUpdatedPayload(listing, next));
            log.info("ListingUpdated published (photo change) listingId=[{}] version=[{}]", listingId, newVersionNo);
        }
    }

    private ListingVersionEntity loadVersionToRead(ListingEntity listing) {
        int versionNo;
        if (listing.getStatus() == ListingStatus.PUBLISHED && listing.getPublishedVersion() != null) {
            versionNo = listing.getPublishedVersion();
        } else {
            versionNo = listing.getCurrentVersion();
        }

        return versionRepository.findByListingIdAndVersionNo(listing.getId(), versionNo)
                .orElseThrow(() -> new IllegalStateException(
                        "Listing version not found for listingId=" + listing.getId() + " versionNo=" + versionNo
                ));
    }

    private ListingUpdatedPayload toUpdatedPayload(ListingEntity listing, ListingVersionEntity version) {
        return ListingUpdatedPayload.builder()
                .listingId(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(listing.getStatus().name())
                .version(version.getVersionNo())
                .title(version.getTitle())
                .description(version.getDescription())
                .priceAmount(version.getPriceAmount())
                .currencyCode(version.getCurrencyCode())
                .address(ListingUpdatedPayload.AddressPayload.builder()
                        .country(version.getAddress() != null ? version.getAddress().getCountry() : null)
                        .city(version.getAddress() != null ? version.getAddress().getCity() : null)
                        .street(version.getAddress() != null ? version.getAddress().getStreet() : null)
                        .postalCode(version.getAddress() != null ? version.getAddress().getPostalCode() : null)
                        .build())
                .area(version.getArea())
                .rooms(version.getRooms())
                .floor(version.getFloor())
                .propertyType(version.getPropertyType() != null ? version.getPropertyType().name() : null)
                .photoIds(version.getPhotoIds())
                .build();
    }
}