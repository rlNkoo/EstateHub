package com.rlnkoo.searchservice.integration.listing;

import com.rlnkoo.searchservice.integration.listing.dto.PublishedListingsPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingServiceClient {

    private final RestClient listingServiceRestClient;

    public PublishedListingsPageResponse getPublishedListingsForReindex(
            int page,
            int size,
            String bearerToken
    ) {
        log.info("Fetching published listings from listing-service page=[{}] size=[{}]", page, size);

        PublishedListingsPageResponse response = listingServiceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/listings/published")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .body(PublishedListingsPageResponse.class);

        if (response == null) {
            log.error("listing-service returned empty response page=[{}] size=[{}]", page, size);
            throw new IllegalStateException("listing-service returned empty response for published listings");
        }

        log.info("Fetched published listings from listing-service page=[{}] size=[{}] items=[{}] totalPages=[{}]",
                page,
                size,
                response.items() != null ? response.items().size() : 0,
                response.totalPages());

        return response;
    }
}