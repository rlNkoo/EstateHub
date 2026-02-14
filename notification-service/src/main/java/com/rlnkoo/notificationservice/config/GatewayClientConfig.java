package com.rlnkoo.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

    @Bean
    RestClient gatewayRestClient(@Value("${notification.gateway-base-url}") String gatewayBaseUrl) {
        return RestClient.builder()
                .baseUrl(gatewayBaseUrl)
                .build();
    }
}