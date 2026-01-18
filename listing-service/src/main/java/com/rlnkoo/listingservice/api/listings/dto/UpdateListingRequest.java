package com.rlnkoo.listingservice.api.listings.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record UpdateListingRequest(

        @NotBlank
        @Size(min = 5, max = 120)
        String title,

        @Size(max = 5000)
        String description,

        @NotNull
        @Positive
        BigDecimal priceAmount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be ISO-4217 (e.g. PLN, EUR, USD)")
        String currencyCode,

        @NotNull
        @Valid
        AddressRequest address,

        @NotNull
        @Positive
        BigDecimal area,

        @Min(1) @Max(50)
        Integer rooms,

        @Min(-10) @Max(300)
        Integer floor,

        @NotBlank
        String propertyType,

        List<UUID> photoIds
) {

    @Builder
    public record AddressRequest(
            @NotBlank @Size(max = 80) String country,
            @NotBlank @Size(max = 120) String city,
            @Size(max = 200) String street,
            @Size(max = 20) String postalCode
    ) {
    }
}