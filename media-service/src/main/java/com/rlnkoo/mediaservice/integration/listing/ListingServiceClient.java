package com.rlnkoo.mediaservice.integration.listing;

import com.rlnkoo.mediaservice.domain.exception.ExternalServiceException;
import com.rlnkoo.mediaservice.domain.exception.ListingNotFoundException;
import com.rlnkoo.mediaservice.integration.listing.dto.ListingDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListingServiceClient {

    private final RestClient listingRestClient;

    public ListingDetailsDto getListing(UUID listingId) {
        try {
            return listingRestClient.get()
                    .uri("/listings/{id}", listingId)
                    .retrieve()
                    .body(ListingDetailsDto.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ListingNotFoundException(listingId);
            }
            throw new ExternalServiceException(
                    "listing-service",
                    "HTTP " + ex.getStatusCode().value() + " when fetching listing " + listingId,
                    ex
            );
        } catch (Exception ex) {
            throw new ExternalServiceException(
                    "listing-service",
                    "Unexpected error when fetching listing " + listingId + ": " + ex.getMessage(),
                    ex
            );
        }
    }
}