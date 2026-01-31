package com.rlnkoo.mediaservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services")
public class ServicesProperties {

    @NotBlank
    private String listing;
}
