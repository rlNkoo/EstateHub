package com.rlnkoo.mediaservice.storage;

import com.rlnkoo.mediaservice.domain.exception.StorageOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectStorageClient implements ObjectStorageClient {

    private final S3Client s3Client;

    @Override
    public void putObject(String bucket, String key, byte[] bytes, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));

            log.info("Object uploaded bucket=[{}] key=[{}] sizeBytes=[{}] contentType=[{}]",
                    bucket, key, bytes.length, contentType);
        } catch (S3Exception ex) {
            log.error("S3 putObject failed bucket=[{}] key=[{}] statusCode=[{}] message=[{}]",
                    bucket, key, ex.statusCode(), ex.getMessage(), ex);
            throw new StorageOperationException("Cannot upload object to storage", ex);
        } catch (Exception ex) {
            log.error("Storage putObject failed bucket=[{}] key=[{}]", bucket, key, ex);
            throw new StorageOperationException("Cannot upload object to storage", ex);
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            log.info("Object deleted bucket=[{}] key=[{}]", bucket, key);
        } catch (S3Exception ex) {
            log.error("S3 deleteObject failed bucket=[{}] key=[{}] statusCode=[{}] message=[{}]",
                    bucket, key, ex.statusCode(), ex.getMessage(), ex);
            throw new StorageOperationException("Cannot delete object from storage", ex);
        } catch (Exception ex) {
            log.error("Storage deleteObject failed bucket=[{}] key=[{}]", bucket, key, ex);
            throw new StorageOperationException("Cannot delete object from storage", ex);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            log.error("S3 headObject failed bucket=[{}] key=[{}] statusCode=[{}] message=[{}]",
                    bucket, key, ex.statusCode(), ex.getMessage(), ex);
            throw new StorageOperationException("Cannot check object existence in storage", ex);
        } catch (Exception ex) {
            log.error("Storage exists check failed bucket=[{}] key=[{}]", bucket, key, ex);
            throw new StorageOperationException("Cannot check object existence in storage", ex);
        }
    }
}