package com.rlnkoo.searchservice.domain.service;

import com.rlnkoo.searchservice.domain.model.SearchListingDocument;
import com.rlnkoo.searchservice.integration.kafka.events.ListingArchivedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingPublishedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingUpdatedV1Payload;
import com.rlnkoo.searchservice.persistence.repository.SearchListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingIndexingService {

    private static final String PUBLISHED = "PUBLISHED";

    private final SearchListingRepository searchListingRepository;

    public void onListingPublished(ListingPublishedV1Payload payload) {
        if (!isPublished(payload.status())) {
            log.warn("Skipping ListingPublishedV1 with non-published status listingId=[{}] status=[{}]",
                    payload.listingId(), payload.status());
            return;
        }

        SearchListingDocument document = SearchListingDocument.builder()
                .id(payload.listingId())
                .ownerId(payload.ownerId())
                .status(payload.status())
                .version(payload.version())
                .publishedAt(payload.publishedAt())
                .title(payload.title())
                .description(payload.description())
                .priceAmount(payload.priceAmount())
                .currencyCode(payload.currencyCode())
                .country(payload.address() != null ? payload.address().country() : null)
                .city(payload.address() != null ? payload.address().city() : null)
                .street(payload.address() != null ? payload.address().street() : null)
                .postalCode(payload.address() != null ? payload.address().postalCode() : null)
                .area(payload.area())
                .rooms(payload.rooms())
                .floor(payload.floor())
                .propertyType(payload.propertyType())
                .photoIds(payload.photoIds() == null ? List.of() : List.copyOf(payload.photoIds()))
                .indexedAt(Instant.now())
                .build();

        searchListingRepository.save(document);

        log.info("Indexed published listing listingId=[{}] version=[{}] status=[{}]",
                payload.listingId(), payload.version(), payload.status());
    }

    public void onListingUpdated(ListingUpdatedV1Payload payload) {
        if (!isPublished(payload.status())) {
            log.warn("Skipping ListingUpdatedV1 with non-published status listingId=[{}] status=[{}]",
                    payload.listingId(), payload.status());
            return;
        }

        SearchListingDocument existing = searchListingRepository.findById(payload.listingId())
                .orElseGet(() -> SearchListingDocument.builder()
                        .id(payload.listingId())
                        .ownerId(payload.ownerId())
                        .publishedAt(null)
                        .build());

        SearchListingDocument document = SearchListingDocument.builder()
                .id(payload.listingId())
                .ownerId(payload.ownerId())
                .status(payload.status())
                .version(payload.version())
                .publishedAt(existing.getPublishedAt())
                .title(payload.title())
                .description(payload.description())
                .priceAmount(payload.priceAmount())
                .currencyCode(payload.currencyCode())
                .country(payload.address() != null ? payload.address().country() : null)
                .city(payload.address() != null ? payload.address().city() : null)
                .street(payload.address() != null ? payload.address().street() : null)
                .postalCode(payload.address() != null ? payload.address().postalCode() : null)
                .area(payload.area())
                .rooms(payload.rooms())
                .floor(payload.floor())
                .propertyType(payload.propertyType())
                .photoIds(payload.photoIds() == null ? List.of() : List.copyOf(payload.photoIds()))
                .indexedAt(Instant.now())
                .build();

        searchListingRepository.save(document);

        log.info("Indexed updated listing listingId=[{}] version=[{}] status=[{}]",
                payload.listingId(), payload.version(), payload.status());
    }

    public void onListingArchived(ListingArchivedV1Payload payload) {
        UUID listingId = payload.listingId();

        if (listingId == null) {
            log.warn("Skipping archive indexing with null listingId");
            return;
        }

        if (searchListingRepository.existsById(listingId)) {
            searchListingRepository.deleteById(listingId);
            log.info("Removed archived listing from index listingId=[{}] version=[{}] status=[{}]",
                    listingId, payload.version(), payload.status());
            return;
        }

        log.debug("Archived listing not found in index listingId=[{}]", listingId);
    }

    public Optional<SearchListingDocument> findById(UUID listingId) {
        return searchListingRepository.findById(listingId);
    }

    public boolean existsById(UUID listingId) {
        return searchListingRepository.existsById(listingId);
    }

    private boolean isPublished(String status) {
        return PUBLISHED.equalsIgnoreCase(status);
    }
}