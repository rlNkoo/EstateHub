package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.config.S3Properties;
import com.rlnkoo.mediaservice.domain.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Properties s3Properties;

    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        when(s3Properties.getBucket()).thenReturn("estatehub-media");
    }

    @Test
    void shouldPutObject() {
        // given
        String objectKey = "listings/123/photos/test.jpg";
        byte[] bytes = "test-bytes".getBytes();
        String contentType = "image/jpeg";

        // when
        assertDoesNotThrow(() -> s3StorageService.putObject(objectKey, bytes, contentType));

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("estatehub-media", request.bucket());
        assertEquals(objectKey, request.key());
        assertEquals(contentType, request.contentType());
    }

    @Test
    void shouldThrowStorageExceptionWhenPutObjectFails() {
        // given
        String objectKey = "listings/123/photos/test.jpg";
        byte[] bytes = "test-bytes".getBytes();
        String contentType = "image/jpeg";

        doThrow(new RuntimeException("S3 upload failed"))
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.putObject(objectKey, bytes, contentType)
        );

        assertEquals(
                "Failed to upload object to storage: key=" + objectKey,
                exception.getMessage()
        );
        assertNotNull(exception.getCause());
        assertEquals("S3 upload failed", exception.getCause().getMessage());
    }

    @Test
    void shouldDeleteObject() {
        // given
        String objectKey = "listings/123/photos/test.jpg";

        // when
        assertDoesNotThrow(() -> s3StorageService.deleteObject(objectKey));

        // then
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertEquals("estatehub-media", request.bucket());
        assertEquals(objectKey, request.key());
    }

    @Test
    void shouldThrowStorageExceptionWhenDeleteObjectFails() {
        // given
        String objectKey = "listings/123/photos/test.jpg";

        doThrow(new RuntimeException("S3 delete failed"))
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.deleteObject(objectKey)
        );

        assertEquals(
                "Failed to delete object from storage: key=" + objectKey,
                exception.getMessage()
        );
        assertNotNull(exception.getCause());
        assertEquals("S3 delete failed", exception.getCause().getMessage());
    }

    @Test
    void shouldReturnPresignedUrl() throws Exception {
        // given
        String objectKey = "listings/123/photos/test.jpg";
        Duration ttl = Duration.ofMinutes(5);
        URL expectedUrl = new URL("http://localhost:9000/estatehub-media/" + objectKey);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);

        when(mediaProperties.getPresignedGetUrlTtl()).thenReturn(ttl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(expectedUrl);

        // when
        URL result = s3StorageService.presignedGetUrl(objectKey);

        // then
        assertEquals(expectedUrl, result);

        ArgumentCaptor<GetObjectPresignRequest> requestCaptor =
                ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(requestCaptor.capture());

        GetObjectPresignRequest presignRequest = requestCaptor.getValue();
        assertEquals(ttl, presignRequest.signatureDuration());

        GetObjectRequest getObjectRequest = presignRequest.getObjectRequest();
        assertEquals("estatehub-media", getObjectRequest.bucket());
        assertEquals(objectKey, getObjectRequest.key());
    }

    @Test
    void shouldThrowStorageExceptionWhenPresigningFails() {
        // given
        String objectKey = "listings/123/photos/test.jpg";
        Duration ttl = Duration.ofMinutes(5);

        when(mediaProperties.getPresignedGetUrlTtl()).thenReturn(ttl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("Presign failed"));

        // when + then
        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.presignedGetUrl(objectKey)
        );

        assertEquals(
                "Failed to create pre-signed GET url: key=" + objectKey,
                exception.getMessage()
        );
        assertNotNull(exception.getCause());
        assertEquals("Presign failed", exception.getCause().getMessage());
    }
}