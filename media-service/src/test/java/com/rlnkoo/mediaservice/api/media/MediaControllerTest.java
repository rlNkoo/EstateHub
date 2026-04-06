package com.rlnkoo.mediaservice.api.media;

import com.rlnkoo.commonsecurity.Claims;
import com.rlnkoo.mediaservice.domain.exception.AuthenticationRequiredException;
import com.rlnkoo.mediaservice.events.producer.MediaEventsPublisher;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import com.rlnkoo.mediaservice.persistence.repository.MediaObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.rlnkoo.mediaservice.domain.service.ListingOwnershipVerifier;
import com.rlnkoo.mediaservice.domain.service.S3StorageService;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaObjectRepository mediaObjectRepository;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private ListingOwnershipVerifier listingOwnershipVerifier;

    @MockitoBean
    private MediaEventsPublisher mediaEventsPublisher;

    @BeforeEach
    void setUp() {
        mediaObjectRepository.deleteAll();
        reset(s3StorageService, listingOwnershipVerifier, mediaEventsPublisher, s3Client);
    }

    @Test
    void shouldReturnUnauthorizedWhenUploadingPhotoWithoutToken() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "image-bytes".getBytes()
        );

        // when + then
        mockMvc.perform(multipart("/media/listings/{listingId}/photos", listingId)
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUploadPhotoWhenAuthenticatedOwner() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(userId);
        when(s3StorageService.presignedGetUrl(anyString()))
                .thenAnswer(invocation -> new URL("http://localhost:9000/" + invocation.getArgument(0)));

        // when + then
        mockMvc.perform(multipart("/media/listings/{listingId}/photos", listingId)
                        .file(file)
                        .with(jwtUser(userId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId", notNullValue()))
                .andExpect(jsonPath("$.listingId").value(listingId.toString()))
                .andExpect(jsonPath("$.url", notNullValue()))
                .andExpect(jsonPath("$.thumbnailUrl", notNullValue()));

        List<MediaObjectEntity> savedPhotos = mediaObjectRepository.findAll();
        assertEquals(1, savedPhotos.size());

        MediaObjectEntity saved = savedPhotos.getFirst();
        assertEquals(listingId, saved.getListingId());
        assertEquals(userId, saved.getOwnerId());
        assertEquals("image/jpeg", saved.getContentType());
        assertEquals("estatehub-media-test", saved.getBucket());
        assertNotNull(saved.getObjectKey());
        assertNotNull(saved.getThumbnailKey());
        assertNotNull(saved.getSha256());
        assertNotNull(saved.getCreatedAt());
        assertNull(saved.getDeletedAt());

        verify(listingOwnershipVerifier).requireOwnerOrAdmin(listingId);
        verify(s3StorageService, times(2)).putObject(anyString(), any(byte[].class), anyString());
        verify(s3StorageService, times(2)).presignedGetUrl(anyString());
        verify(mediaEventsPublisher).publishPhotoUploaded(eq(listingId), any());
    }

    @Test
    void shouldReturnBadRequestWhenContentTypeIsInvalid() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.gif",
                "image/gif",
                "image-bytes".getBytes()
        );

        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(userId);

        // when + then
        mockMvc.perform(multipart("/media/listings/{listingId}/photos", listingId)
                        .file(file)
                        .with(jwtUser(userId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid content type: image/gif"))
                .andExpect(jsonPath("$.path").value("/media/listings/" + listingId + "/photos"));

        assertEquals(0, mediaObjectRepository.count());
        verify(mediaEventsPublisher, never()).publishPhotoUploaded(any(), any());
    }

    @Test
    void shouldReturnPayloadTooLargeWhenFileTooLarge() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        byte[] tooLargeBytes = new byte[5_242_881];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                tooLargeBytes
        );

        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(userId);

        // when + then
        mockMvc.perform(multipart("/media/listings/{listingId}/photos", listingId)
                        .file(file)
                        .with(jwtUser(userId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.error").value("Payload Too Large"))
                .andExpect(jsonPath("$.message")
                        .value("File too large: 5242881 bytes (max=5242880)"))
                .andExpect(jsonPath("$.path").value("/media/listings/" + listingId + "/photos"));

        assertEquals(0, mediaObjectRepository.count());
        verify(mediaEventsPublisher, never()).publishPhotoUploaded(any(), any());
    }

    @Test
    void shouldReturnBadRequestWhenMultipartFileIsMissing() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when + then
        mockMvc.perform(multipart("/media/listings/{listingId}/photos", listingId)
                        .with(jwtUser(userId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/media/listings/" + listingId + "/photos"));
    }

    @Test
    void shouldReturnListingPhotosWhenListingIsReadableWithoutToken() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();

        MediaObjectEntity photo = MediaObjectEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .ownerId(UUID.randomUUID())
                .bucket("estatehub-media-test")
                .objectKey("listings/" + listingId + "/photos/photo1.jpg")
                .thumbnailKey("listings/" + listingId + "/thumbs/photo1.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1234L)
                .sha256("hash-123")
                .createdAt(Instant.now())
                .build();

        mediaObjectRepository.save(photo);

        doNothing().when(listingOwnershipVerifier).requireCanRead(listingId);
        when(s3StorageService.presignedGetUrl(anyString()))
                .thenAnswer(invocation -> new URL("http://localhost:9000/" + invocation.getArgument(0)));

        // when + then
        mockMvc.perform(get("/media/listings/{listingId}/photos", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mediaId").value(photo.getId().toString()))
                .andExpect(jsonPath("$[0].listingId").value(listingId.toString()))
                .andExpect(jsonPath("$[0].contentType").value("image/jpeg"))
                .andExpect(jsonPath("$[0].sizeBytes").value(1234))
                .andExpect(jsonPath("$[0].sha256").value("hash-123"))
                .andExpect(jsonPath("$[0].url", notNullValue()))
                .andExpect(jsonPath("$[0].thumbnailUrl", notNullValue()));

        verify(listingOwnershipVerifier).requireCanRead(listingId);
    }

    @Test
    void shouldReturnUnauthorizedWhenListingIsNotPublishedAndUserIsAnonymous() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();

        doThrow(new AuthenticationRequiredException())
                .when(listingOwnershipVerifier)
                .requireCanRead(listingId);

        // when + then
        mockMvc.perform(get("/media/listings/{listingId}/photos", listingId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/media/listings/" + listingId + "/photos"));
    }

    @Test
    void shouldReturnNotFoundWhenPhotoDoesNotExist() throws Exception {
        // given
        UUID mediaId = UUID.randomUUID();

        // when + then
        mockMvc.perform(get("/media/photos/{mediaId}", mediaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Media not found: " + mediaId))
                .andExpect(jsonPath("$.path").value("/media/photos/" + mediaId));
    }

    @Test
    void shouldReturnUnauthorizedWhenDeletingPhotoWithoutToken() throws Exception {
        // given
        UUID mediaId = UUID.randomUUID();

        // when + then
        mockMvc.perform(delete("/media/photos/{mediaId}", mediaId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldDeletePhotoWhenRequesterIsOwner() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        MediaObjectEntity photo = MediaObjectEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .ownerId(ownerId)
                .bucket("estatehub-media-test")
                .objectKey("listings/" + listingId + "/photos/photo1.jpg")
                .thumbnailKey("listings/" + listingId + "/thumbs/photo1.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1234L)
                .sha256("hash-123")
                .createdAt(Instant.now())
                .build();

        mediaObjectRepository.save(photo);

        // when + then
        mockMvc.perform(delete("/media/photos/{mediaId}", photo.getId())
                        .with(jwtUser(ownerId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isNoContent());

        MediaObjectEntity updated = mediaObjectRepository.findById(photo.getId()).orElseThrow();
        assertNotNull(updated.getDeletedAt());

        verify(s3StorageService).deleteObject(photo.getObjectKey());
        verify(s3StorageService).deleteObject(photo.getThumbnailKey());
        verify(mediaEventsPublisher).publishPhotoDeleted(eq(listingId), any());
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotOwnerAndNotAdmin() throws Exception {
        // given
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        MediaObjectEntity photo = MediaObjectEntity.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .ownerId(ownerId)
                .bucket("estatehub-media-test")
                .objectKey("listings/" + listingId + "/photos/photo1.jpg")
                .thumbnailKey("listings/" + listingId + "/thumbs/photo1.jpg")
                .contentType("image/jpeg")
                .sizeBytes(1234L)
                .sha256("hash-123")
                .createdAt(Instant.now())
                .build();

        mediaObjectRepository.save(photo);

        // when + then
        mockMvc.perform(delete("/media/photos/{mediaId}", photo.getId())
                        .with(jwtUser(strangerId, "stranger@example.com", List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied for media: " + photo.getId()))
                .andExpect(jsonPath("$.path").value("/media/photos/" + photo.getId()));

        MediaObjectEntity unchanged = mediaObjectRepository.findById(photo.getId()).orElseThrow();
        assertNull(unchanged.getDeletedAt());

        verify(s3StorageService, never()).deleteObject(anyString());
        verify(mediaEventsPublisher, never()).publishPhotoDeleted(any(), any());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingPhotoDoesNotExist() throws Exception {
        // given
        UUID mediaId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when + then
        mockMvc.perform(delete("/media/photos/{mediaId}", mediaId)
                        .with(jwtUser(userId, "owner@example.com", List.of("USER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Media not found: " + mediaId))
                .andExpect(jsonPath("$.path").value("/media/photos/" + mediaId));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtUser(
            UUID userId, String email, List<String> roles
    ) {
        return jwt().jwt(jwt -> jwt
                .subject(userId.toString())
                .claim(Claims.EMAIL, email)
                .claim(Claims.ROLES, roles)
        );
    }

    private byte[] createTestImageBytes() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);

        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.BLUE);
            graphics.fillRect(0, 0, 100, 100);
        } finally {
            graphics.dispose();
        }

        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        boolean written = javax.imageio.ImageIO.write(image, "jpg", outputStream);
        assertTrue(written);

        return outputStream.toByteArray();
    }
}