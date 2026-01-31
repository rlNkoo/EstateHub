package com.rlnkoo.mediaservice.storage;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.domain.exception.StorageOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer {

    private final S3Client s3Client;
    private final MediaProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        String bucket = properties.getBucket();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket exists bucket=[{}]", bucket);
        } catch (NoSuchBucketException ex) {
            createBucket(bucket);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                createBucket(bucket);
                return;
            }
            log.error("Head bucket failed bucket=[{}] statusCode=[{}] message=[{}]",
                    bucket, ex.statusCode(), ex.getMessage(), ex);
            throw new StorageOperationException("Cannot check bucket existence", ex);
        } catch (Exception ex) {
            log.error("Bucket check failed bucket=[{}]", bucket, ex);
            throw new StorageOperationException("Cannot check bucket existence", ex);
        }
    }

    private void createBucket(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket created bucket=[{}]", bucket);
        } catch (S3Exception ex) {
            log.error("Create bucket failed bucket=[{}] statusCode=[{}] message=[{}]",
                    bucket, ex.statusCode(), ex.getMessage(), ex);
            throw new StorageOperationException("Cannot create bucket", ex);
        } catch (Exception ex) {
            log.error("Create bucket failed bucket=[{}]", bucket, ex);
            throw new StorageOperationException("Cannot create bucket", ex);
        }
    }
}