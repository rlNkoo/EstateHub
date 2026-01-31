package com.rlnkoo.mediaservice.config;

import com.rlnkoo.mediaservice.domain.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @PostConstruct
    public void ensureBucketExists() {
        String bucket = s3Properties.getBucket();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 bucket exists bucket=[{}]", bucket);
        } catch (NoSuchBucketException ex) {
            log.warn("S3 bucket does not exist, creating bucket=[{}]", bucket);
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("S3 bucket created bucket=[{}]", bucket);
            } catch (Exception createEx) {
                throw new StorageException("Cannot create bucket: " + bucket, createEx);
            }
        } catch (Exception ex) {
            throw new StorageException("Cannot access bucket: " + bucket, ex);
        }
    }
}