package com.rlnkoo.searchservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final ListingServiceProperties listingServiceProperties;

    @Bean
    public RestClient listingServiceRestClient() {
        return RestClient.builder()
                .baseUrl(listingServiceProperties.getBaseUrl())
                .build();
    }
}