package com.rlnkoo.mediaservice.config;

import com.rlnkoo.mediaservice.domain.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BucketInitializerTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private BucketInitializer bucketInitializer;

    @Test
    void shouldDoNothingWhenBucketAlreadyExists() {
        // given
        when(s3Properties.getBucket()).thenReturn("estatehub-media");

        // when
        assertDoesNotThrow(() -> bucketInitializer.ensureBucketExists());

        // then
        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void shouldCreateBucketWhenBucketDoesNotExist() {
        // given
        when(s3Properties.getBucket()).thenReturn("estatehub-media");

        NoSuchBucketException noSuchBucketException = NoSuchBucketException.builder()
                .message("Bucket does not exist")
                .build();

        doThrow(noSuchBucketException)
                .when(s3Client)
                .headBucket(any(HeadBucketRequest.class));

        // when
        assertDoesNotThrow(() -> bucketInitializer.ensureBucketExists());

        // then
        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void shouldThrowStorageExceptionWhenBucketAccessFails() {
        // given
        when(s3Properties.getBucket()).thenReturn("estatehub-media");

        doThrow(new RuntimeException("S3 unavailable"))
                .when(s3Client)
                .headBucket(any(HeadBucketRequest.class));

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> bucketInitializer.ensureBucketExists()
        );

        assertEquals("Cannot access bucket: estatehub-media", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("S3 unavailable", exception.getCause().getMessage());

        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void shouldThrowStorageExceptionWhenBucketCreationFails() {
        // given
        when(s3Properties.getBucket()).thenReturn("estatehub-media");

        NoSuchBucketException noSuchBucketException = NoSuchBucketException.builder()
                .message("Bucket does not exist")
                .build();

        doThrow(noSuchBucketException)
                .when(s3Client)
                .headBucket(any(HeadBucketRequest.class));

        doThrow(new RuntimeException("Create bucket failed"))
                .when(s3Client)
                .createBucket(any(CreateBucketRequest.class));

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> bucketInitializer.ensureBucketExists()
        );

        assertEquals("Cannot create bucket: estatehub-media", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Create bucket failed", exception.getCause().getMessage());

        verify(s3Client).headBucket(any(HeadBucketRequest.class));
        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }
}