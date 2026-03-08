package com.rlnkoo.searchservice.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.integration.listing-service")
public class ListingServiceProperties {

    @NotBlank
    private String baseUrl;
}