package com.rlnkoo.listingservice.domain.model;

import java.util.Objects;

public record Address(
        String country,
        String city,
        String street,
        String postalCode
) {

    public Address {
        Objects.requireNonNull(country, "Country must not be null");
        Objects.requireNonNull(city, "City must not be null");

        if (country.isBlank()) {
            throw new IllegalArgumentException("Country must not be blank");
        }
        if (city.isBlank()) {
            throw new IllegalArgumentException("City must not be blank");
        }

        if (street != null && street.isBlank()) {
            throw new IllegalArgumentException("Street must not be blank");
        }
        if (postalCode != null && postalCode.isBlank()) {
            throw new IllegalArgumentException("PostalCode must not be blank");
        }
    }
}