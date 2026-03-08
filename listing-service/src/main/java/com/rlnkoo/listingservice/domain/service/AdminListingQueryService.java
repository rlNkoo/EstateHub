package com.rlnkoo.listingservice.domain.service;

import com.rlnkoo.listingservice.api.admin.dto.PublishedListingForReindexResponse;
import com.rlnkoo.listingservice.api.admin.dto.PublishedListingsPageResponse;
import com.rlnkoo.listingservice.domain.exception.InvalidAdminListingQueryException;
import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.persistence.repository.ListingRepository;
import com.rlnkoo.listingservice.persistence.repository.ListingVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminListingQueryService {

    private final ListingRepository listingRepository;
    private final ListingVersionRepository listingVersionRepository;

    @Transactional(readOnly = true)
    public PublishedListingsPageResponse getPublishedListingsForReindex(int page, int size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        log.info("Published listings for reindex request page=[{}] size=[{}]",
                normalizedPage, normalizedSize);

        Page<ListingEntity> listingsPage = listingRepository.findAllByStatusOrderByPublishedAtDesc(
                ListingStatus.PUBLISHED,
                PageRequest.of(normalizedPage, normalizedSize)
        );

        List<PublishedListingForReindexResponse> items = new ArrayList<>();

        for (ListingEntity listing : listingsPage.getContent()) {
            Integer publishedVersion = listing.getPublishedVersion();

            if (publishedVersion == null) {
                log.warn("Skipping published listing without publishedVersion listingId=[{}]", listing.getId());
                continue;
            }

            ListingVersionEntity version = listingVersionRepository
                    .findByListingIdAndVersionNo(listing.getId(), publishedVersion)
                    .orElse(null);

            if (version == null) {
                log.warn("Skipping published listing without content listingId=[{}] version=[{}]",
                        listing.getId(), publishedVersion);
                continue;
            }

            items.add(mapToPublishedListingResponse(listing, version, publishedVersion));
        }

        log.info("Published listings for reindex resolved page=[{}] size=[{}] items=[{}] totalElements=[{}]",
                normalizedPage, normalizedSize, items.size(), listingsPage.getTotalElements());

        return PublishedListingsPageResponse.builder()
                .items(items)
                .totalElements(listingsPage.getTotalElements())
                .totalPages(listingsPage.getTotalPages())
                .page(listingsPage.getNumber())
                .size(listingsPage.getSize())
                .build();
    }

    private PublishedListingForReindexResponse mapToPublishedListingResponse(
            ListingEntity listing,
            ListingVersionEntity version,
            int publishedVersion
    ) {
        return PublishedListingForReindexResponse.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(listing.getStatus().name())
                .version(publishedVersion)
                .publishedAt(listing.getPublishedAt())
                .updatedAt(listing.getUpdatedAt())
                .title(version.getTitle())
                .description(version.getDescription())
                .priceAmount(version.getPriceAmount())
                .currencyCode(version.getCurrencyCode())
                .address(PublishedListingForReindexResponse.AddressResponse.builder()
                        .country(version.getAddress() != null ? version.getAddress().getCountry() : null)
                        .city(version.getAddress() != null ? version.getAddress().getCity() : null)
                        .street(version.getAddress() != null ? version.getAddress().getStreet() : null)
                        .postalCode(version.getAddress() != null ? version.getAddress().getPostalCode() : null)
                        .build())
                .area(version.getArea())
                .rooms(version.getRooms())
                .floor(version.getFloor())
                .propertyType(version.getPropertyType() != null ? version.getPropertyType().name() : null)
                .photoIds(version.getPhotoIds() == null ? List.of() : List.copyOf(version.getPhotoIds()))
                .build();
    }

    private int normalizePage(int page) {
        if (page < 0) {
            throw new InvalidAdminListingQueryException("page must be greater than or equal to 0");
        }
        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            throw new InvalidAdminListingQueryException("size must be greater than 0");
        }

        if (size > 500) {
            throw new InvalidAdminListingQueryException("size must be less than or equal to 500");
        }

        return size;
    }
}