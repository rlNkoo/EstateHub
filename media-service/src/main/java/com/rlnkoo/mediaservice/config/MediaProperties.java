package com.rlnkoo.mediaservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    @Min(1)
    private int maxPhotosPerListing;

    @Min(1)
    private long maxFileSizeBytes;

    @NotNull
    private List<@NotBlank String> allowedContentTypes;

    @NotNull
    private Duration presignedGetUrlTtl;
}