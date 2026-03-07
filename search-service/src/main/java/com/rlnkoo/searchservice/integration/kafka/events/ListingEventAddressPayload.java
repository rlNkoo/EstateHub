package com.rlnkoo.searchservice.integration.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListingEventAddressPayload(
        String country,
        String city,
        String street,
        String postalCode
) {
}