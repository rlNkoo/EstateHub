package com.rlnkoo.mediaservice.integration.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.mediaservice.domain.exception.ExternalServiceException;
import com.rlnkoo.mediaservice.domain.exception.ListingNotFoundException;
import com.rlnkoo.mediaservice.integration.listing.dto.ListingDetailsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingServiceClient {

    private final RestClient listingRestClient;
    private final ObjectMapper objectMapper;

    public ListingDetailsDto getListing(UUID listingId) {
        try {
            ResponseEntity<String> raw = listingRestClient.get()
                    .uri("/listings/{id}", listingId)
                    .retrieve()
                    .toEntity(String.class);

            log.info("ListingServiceClient response listingId=[{}] httpStatus=[{}] body={}",
                    listingId, raw.getStatusCode(), raw.getBody());

            return objectMapper.readValue(raw.getBody(), ListingDetailsDto.class);

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