package com.rlnkoo.listingservice.api.listings;

import com.rlnkoo.listingservice.api.listings.dto.*;
import com.rlnkoo.listingservice.domain.exception.ListingContentNotFoundException;
import com.rlnkoo.listingservice.domain.exception.ListingNotFoundException;
import com.rlnkoo.listingservice.domain.model.ListingStatus;
import com.rlnkoo.listingservice.domain.service.ListingService;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingsController {

    private final ListingService listingService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateListingResponse createDraft() {
        UUID id = listingService.createDraft();
        return CreateListingResponse.builder().id(id).build();
    }

    @PutMapping("/{id}")
    public ListingActionResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        ListingEntity listing = listingService.update(id, request);

        int version = listing.getStatus() == ListingStatus.PUBLISHED
                ? (listing.getPublishedVersion() != null ? listing.getPublishedVersion() : listing.getCurrentVersion())
                : listing.getCurrentVersion();

        return ListingActionResponse.builder()
                .id(listing.getId())
                .status(listing.getStatus().name())
                .version(version)
                .build();
    }

    @PostMapping("/{id}/publish")
    public ListingActionResponse publish(@PathVariable("id") UUID id) {
        ListingEntity listing = listingService.publish(id);
        return ListingActionResponse.builder()
                .id(listing.getId())
                .status(listing.getStatus().name())
                .version(listing.getPublishedVersion() != null ? listing.getPublishedVersion() : listing.getCurrentVersion())
                .build();
    }

    @PostMapping("/{id}/archive")
    public ListingActionResponse archive(@PathVariable("id") UUID id) {
        ListingEntity listing = listingService.archive(id);
        return ListingActionResponse.builder()
                .id(listing.getId())
                .status(listing.getStatus().name())
                .version(listing.getCurrentVersion())
                .build();
    }

    @GetMapping("/{id}")
    public ListingDetailsResponse getListing(@PathVariable("id") UUID id) {
        ListingEntity listing = listingService.getListing(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        boolean isOwnerOrAdmin = currentUserProvider.getCurrentUserOptional()
                .map(u -> u.userId().equals(listing.getOwnerId()) || u.roles().contains("ADMIN"))
                .orElse(false);

        if (!listing.getStatus().isPubliclyVisible()) {
            if (!isOwnerOrAdmin) {
                throw new ListingNotFoundException(id);
            }
        }

        int versionToRead = listingService.resolveVersionForRead(listing);

        ListingVersionEntity version = listingService.getListingVersion(id, versionToRead)
                .orElseThrow(() -> new ListingContentNotFoundException(id));

        return toDetailsResponse(listing, version, versionToRead);
    }

    @GetMapping("/mine")
    public List<ListingSummaryResponse> myListings() {
        List<ListingEntity> listings = listingService.getMyListings();

        List<ListingSummaryResponse> result = new ArrayList<>();
        for (ListingEntity listing : listings) {
            result.add(ListingSummaryResponse.builder()
                    .id(listing.getId())
                    .status(listing.getStatus().name())
                    .version(listing.getCurrentVersion())
                    .updatedAt(listing.getUpdatedAt())
                    .build());
        }
        return result;
    }

    private ListingDetailsResponse toDetailsResponse(ListingEntity listing, ListingVersionEntity version, int versionNo) {
        return ListingDetailsResponse.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(listing.getStatus().name())
                .version(versionNo)
                .title(version.getTitle())
                .description(version.getDescription())
                .priceAmount(version.getPriceAmount())
                .currencyCode(version.getCurrencyCode())
                .address(ListingDetailsResponse.AddressResponse.builder()
                        .country(version.getAddress().getCountry())
                        .city(version.getAddress().getCity())
                        .street(version.getAddress().getStreet())
                        .postalCode(version.getAddress().getPostalCode())
                        .build())
                .area(version.getArea())
                .rooms(version.getRooms())
                .floor(version.getFloor())
                .propertyType(version.getPropertyType().name())
                .photoIds(version.getPhotoIds())
                .build();
    }
}