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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ListingSearchService listingSearchService;

    @GetMapping("/listings")
    public SearchListingsResponse searchListings(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "propertyType", required = false) String propertyType,
            @RequestParam(name = "priceFrom", required = false) BigDecimal priceFrom,
            @RequestParam(name = "priceTo", required = false) BigDecimal priceTo,
            @RequestParam(name = "areaFrom", required = false) BigDecimal areaFrom,
            @RequestParam(name = "areaTo", required = false) BigDecimal areaTo,
            @RequestParam(name = "rooms", required = false) Integer rooms,
            @RequestParam(name = "floor", required = false) Integer floor,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort
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
        return listingSearchService.getById(listingId);
    }
}