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
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseGet(() -> {
                    log.warn("PhotoUploaded ignored: listing not found listingId=[{}] mediaId=[{}]", listingId, mediaId);
                    return null;
                });

        if (listing == null) return;

        ListingVersionEntity version = loadVersionToUpdate(listing);

        int before = version.getPhotoIds() == null ? 0 : version.getPhotoIds().size();
        version.addPhotoId(mediaId);
        versionRepository.save(version);

        int after = version.getPhotoIds() == null ? 0 : version.getPhotoIds().size();
        log.info("Photo added listingId=[{}] versionNo=[{}] mediaId=[{}] {}->{}",
                listingId, version.getVersionNo(), mediaId, before, after);

        if (listing.getStatus() == ListingStatus.PUBLISHED) {
            eventsPublisher.publishListingUpdated(
                    listingId,
                    toUpdatedPayload(listing, version)
            );
            log.info("ListingUpdated published (photo added) listingId=[{}] version=[{}]", listingId, version.getVersionNo());
        }
    }

    @Transactional
    public void onPhotoDeleted(UUID listingId, UUID mediaId) {
        ListingEntity listing = listingRepository.findById(listingId)
                .orElseGet(() -> {
                    log.warn("PhotoDeleted ignored: listing not found listingId=[{}] mediaId=[{}]", listingId, mediaId);
                    return null;
                });

        if (listing == null) return;

        ListingVersionEntity version = loadVersionToUpdate(listing);

        int before = version.getPhotoIds() == null ? 0 : version.getPhotoIds().size();
        version.removePhotoId(mediaId);
        versionRepository.save(version);

        int after = version.getPhotoIds() == null ? 0 : version.getPhotoIds().size();
        log.info("Photo removed listingId=[{}] versionNo=[{}] mediaId=[{}] {}->{}",
                listingId, version.getVersionNo(), mediaId, before, after);

        if (listing.getStatus() == ListingStatus.PUBLISHED) {
            eventsPublisher.publishListingUpdated(
                    listingId,
                    toUpdatedPayload(listing, version)
            );
            log.info("ListingUpdated published (photo removed) listingId=[{}] version=[{}]", listingId, version.getVersionNo());
        }
    }

    private ListingVersionEntity loadVersionToUpdate(ListingEntity listing) {
        int versionNoToUpdate;

        if (listing.getStatus() == ListingStatus.PUBLISHED && listing.getPublishedVersion() != null) {
            versionNoToUpdate = listing.getPublishedVersion();
        } else {
            versionNoToUpdate = listing.getCurrentVersion();
        }

        return versionRepository.findByListingIdAndVersionNo(listing.getId(), versionNoToUpdate)
                .orElseThrow(() -> new IllegalStateException(
                        "Listing version not found for listingId=" + listing.getId() + " versionNo=" + versionNoToUpdate
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