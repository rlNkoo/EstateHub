package com.rlnkoo.mediaservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s3")
public class MediaProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;
}