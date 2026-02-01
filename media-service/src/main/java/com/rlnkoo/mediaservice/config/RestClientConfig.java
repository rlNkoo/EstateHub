package com.rlnkoo.mediaservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpRequestInterceptor;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    @Bean
    public RestClient listingRestClient(ServicesProperties servicesProperties) {

        ClientHttpRequestInterceptor bearerForwardingInterceptor = (request, body, execution) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue);
            }

            return execution.execute(request, body);
        };

        return RestClient.builder()
                .baseUrl(servicesProperties.getListing())
                .requestInterceptor(bearerForwardingInterceptor)
                .build();
    }
}