package com.rlnkoo.searchservice.api.search;

import com.rlnkoo.searchservice.api.search.dto.SearchListingItemResponse;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsRequest;
import com.rlnkoo.searchservice.api.search.dto.SearchListingsResponse;
import com.rlnkoo.searchservice.domain.service.ListingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ListingSearchService listingSearchService;

    @GetMapping("/listings")
    public SearchListingsResponse searchListings(
            String q,
            String city,
            String country,
            String propertyType,
            BigDecimal priceFrom,
            BigDecimal priceTo,
            BigDecimal areaFrom,
            BigDecimal areaTo,
            Integer rooms,
            Integer floor,
            Integer page,
            Integer size,
            String sort
    ) {
        SearchListingsRequest request = SearchListingsRequest.builder()
                .q(q)
                .city(city)
                .country(country)
                .propertyType(propertyType)
                .priceFrom(priceFrom)
                .priceTo(priceTo)
                .areaFrom(areaFrom)
                .areaTo(areaTo)
                .rooms(rooms)
                .floor(floor)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        return listingSearchService.search(request);
    }

    @GetMapping("/listings/{listingId}")
    public SearchListingItemResponse getListing(@PathVariable("listingId") UUID listingId) {
        SearchListingItemResponse response = listingSearchService.getByIdOrNull(listingId);
        if (response == null) {
            throw new ResponseStatusException(NOT_FOUND, "Listing not found");
        }
        return response;
    }
}