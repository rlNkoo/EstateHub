package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.config.S3Properties;
import com.rlnkoo.mediaservice.domain.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final MediaProperties mediaProperties;

    public void putObject(String objectKey, byte[] bytes, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (Exception ex) {
            throw new StorageException("Failed to upload object to storage: key=" + objectKey, ex);
        }
    }

    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(request);
        } catch (Exception ex) {
            throw new StorageException("Failed to delete object from storage: key=" + objectKey, ex);
        }
    }

    public URL presignedGetUrl(String objectKey) {
        try {
            Duration ttl = mediaProperties.getPresignedGetUrlTtl();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url();
        } catch (Exception ex) {
            throw new StorageException("Failed to create pre-signed GET url: key=" + objectKey, ex);
        }
    }
}