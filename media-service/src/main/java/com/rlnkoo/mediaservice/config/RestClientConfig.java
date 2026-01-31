package com.rlnkoo.mediaservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    @Bean
    public RestClient listingRestClient(ServicesProperties servicesProperties) {
        return RestClient.builder()
                .baseUrl(servicesProperties.getListing())
                .build();
    }
}