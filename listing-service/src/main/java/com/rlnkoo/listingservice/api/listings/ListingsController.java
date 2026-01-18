package com.rlnkoo.listingservice.api.listings;

import com.rlnkoo.listingservice.api.listings.dto.CreateListingResponse;
import com.rlnkoo.listingservice.api.listings.dto.ListingDetailsResponse;
import com.rlnkoo.listingservice.api.listings.dto.ListingSummaryResponse;
import com.rlnkoo.listingservice.api.listings.dto.UpdateListingRequest;
import com.rlnkoo.listingservice.domain.exception.ListingContentNotFoundException;
import com.rlnkoo.listingservice.domain.exception.ListingNotFoundException;
import com.rlnkoo.listingservice.domain.service.ListingService;
import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import com.rlnkoo.listingservice.security.CurrentUser;
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        listingService.updateDraft(id, request);
    }

    @PostMapping("/{id}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@PathVariable UUID id) {
        listingService.publish(id);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        listingService.archive(id);
    }

    @GetMapping("/{id}")
    public ListingDetailsResponse getListing(@PathVariable UUID id) {
        ListingEntity listing = listingService.getListing(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (!listing.getStatus().isPubliclyVisible()) {
            CurrentUser user = currentUserProvider.getCurrentUserOptional()
                    .orElseThrow(() -> new ListingNotFoundException(id));

            boolean isOwner = user.userId().equals(listing.getOwnerId());
            boolean isAdmin = user.roles().contains("ADMIN");

            if (!isOwner && !isAdmin) {
                throw new ListingNotFoundException(id);
            }
        }

        ListingVersionEntity version = listingService.getListingCurrentVersion(id)
                .orElseThrow(() -> new ListingContentNotFoundException(id));

        return toDetailsResponse(listing, version);
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

    private ListingDetailsResponse toDetailsResponse(ListingEntity listing, ListingVersionEntity version) {
        return ListingDetailsResponse.builder()
                .id(listing.getId())
                .ownerId(listing.getOwnerId())
                .status(listing.getStatus().name())
                .version(listing.getCurrentVersion())
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