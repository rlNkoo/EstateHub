package com.rlnkoo.mediaservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MediaProperties.class)
public class AwsS3Config {

    private final MediaProperties properties;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}