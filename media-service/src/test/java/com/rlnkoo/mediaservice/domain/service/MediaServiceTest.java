package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.config.S3Properties;
import com.rlnkoo.mediaservice.domain.exception.MediaNotFoundException;
import com.rlnkoo.mediaservice.domain.exception.MediaOwnershipException;
import com.rlnkoo.mediaservice.domain.exception.PhotoLimitExceededException;
import com.rlnkoo.mediaservice.domain.exception.StorageException;
import com.rlnkoo.mediaservice.events.producer.MediaEventsPublisher;
import com.rlnkoo.mediaservice.events.types.PhotoDeletedPayload;
import com.rlnkoo.mediaservice.events.types.PhotoUploadedPayload;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import com.rlnkoo.mediaservice.persistence.repository.MediaObjectRepository;
import com.rlnkoo.mediaservice.security.CurrentUser;
import com.rlnkoo.mediaservice.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaProperties mediaProperties;

    @Mock
    private S3Properties s3Properties;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ListingOwnershipVerifier listingOwnershipVerifier;

    @Mock
    private MediaObjectRepository mediaObjectRepository;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private HashingService hashingService;

    @Mock
    private ContentTypeMapper contentTypeMapper;

    @Mock
    private ObjectKeyFactory objectKeyFactory;

    @Mock
    private ImageProcessingService imageProcessingService;

    @Mock
    private S3StorageService storageService;

    @Mock
    private MediaEventsPublisher eventsPublisher;

    @InjectMocks
    private MediaService mediaService;

    private UUID listingId;
    private UUID ownerId;
    private UUID requesterId;
    private UUID mediaId;
    private CurrentUser currentUser;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        requesterId = ownerId;
        mediaId = UUID.randomUUID();

        currentUser = CurrentUser.builder()
                .userId(requesterId)
                .email("owner@example.com")
                .roles(Set.of("USER"))
                .build();

        file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "test-image-bytes".getBytes()
        );
    }

    @Test
    void shouldUploadPhotoSaveEntityAndPublishEvent() throws IOException {
        // given
        byte[] bytes = file.getBytes();
        byte[] thumbnailBytes = "thumb-bytes".getBytes();
        String sha256 = "sha256-hash";
        String objectKey = "listings/" + listingId + "/photos/" + mediaId + ".jpg";
        String thumbnailKey = "listings/" + listingId + "/thumbs/" + mediaId + ".jpg";

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(ownerId);
        when(mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId)).thenReturn(3L);
        when(mediaProperties.getMaxPhotosPerListing()).thenReturn(15);
        when(hashingService.sha256Hex(bytes)).thenReturn(sha256);
        when(contentTypeMapper.extensionFor("image/jpeg")).thenReturn(".jpg");
        when(objectKeyFactory.photoObjectKey(listingId, mediaId, ".jpg")).thenReturn(objectKey);
        when(objectKeyFactory.thumbnailObjectKey(listingId, mediaId)).thenReturn(thumbnailKey);
        when(imageProcessingService.createJpegThumbnail(bytes)).thenReturn(thumbnailBytes);
        when(s3Properties.getBucket()).thenReturn("estatehub-media");

        when(mediaObjectRepository.save(any(MediaObjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (var mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(mediaId);

            // when
            MediaObjectEntity result = mediaService.uploadPhoto(listingId, file);

            // then
            assertNotNull(result);
            assertEquals(mediaId, result.getId());
            assertEquals(listingId, result.getListingId());
            assertEquals(ownerId, result.getOwnerId());
            assertEquals("estatehub-media", result.getBucket());
            assertEquals(objectKey, result.getObjectKey());
            assertEquals(thumbnailKey, result.getThumbnailKey());
            assertEquals("image/jpeg", result.getContentType());
            assertEquals(bytes.length, result.getSizeBytes());
            assertEquals(sha256, result.getSha256());
            assertNotNull(result.getCreatedAt());

            verify(currentUserProvider).requireCurrentUser();
            verify(listingOwnershipVerifier).requireOwnerOrAdmin(listingId);
            verify(mediaObjectRepository).countByListingIdAndDeletedAtIsNull(listingId);
            verify(fileValidationService).validate(file);
            verify(hashingService).sha256Hex(bytes);
            verify(contentTypeMapper).extensionFor("image/jpeg");
            verify(objectKeyFactory).photoObjectKey(listingId, mediaId, ".jpg");
            verify(objectKeyFactory).thumbnailObjectKey(listingId, mediaId);
            verify(storageService).putObject(objectKey, bytes, "image/jpeg");
            verify(storageService).putObject(thumbnailKey, thumbnailBytes, "image/jpeg");

            ArgumentCaptor<MediaObjectEntity> entityCaptor = ArgumentCaptor.forClass(MediaObjectEntity.class);
            verify(mediaObjectRepository).save(entityCaptor.capture());
            MediaObjectEntity savedEntity = entityCaptor.getValue();

            assertEquals(mediaId, savedEntity.getId());
            assertEquals(listingId, savedEntity.getListingId());
            assertEquals(ownerId, savedEntity.getOwnerId());
            assertEquals("estatehub-media", savedEntity.getBucket());
            assertEquals(objectKey, savedEntity.getObjectKey());
            assertEquals(thumbnailKey, savedEntity.getThumbnailKey());
            assertEquals("image/jpeg", savedEntity.getContentType());
            assertEquals(bytes.length, savedEntity.getSizeBytes());
            assertEquals(sha256, savedEntity.getSha256());
            assertNotNull(savedEntity.getCreatedAt());

            ArgumentCaptor<PhotoUploadedPayload> payloadCaptor = ArgumentCaptor.forClass(PhotoUploadedPayload.class);
            verify(eventsPublisher).publishPhotoUploaded(eq(listingId), payloadCaptor.capture());
            PhotoUploadedPayload payload = payloadCaptor.getValue();

            assertEquals(mediaId, payload.mediaId());
            assertEquals(listingId, payload.listingId());
            assertEquals(ownerId, payload.ownerId());
            assertEquals("estatehub-media", payload.bucket());
            assertEquals(objectKey, payload.objectKey());
            assertEquals(thumbnailKey, payload.thumbnailKey());
            assertEquals("image/jpeg", payload.contentType());
            assertEquals(bytes.length, payload.sizeBytes());
            assertEquals(sha256, payload.sha256());
            assertNotNull(payload.uploadedAt());
        }
    }

    @Test
    void shouldThrowPhotoLimitExceededExceptionWhenListingAlreadyHasMaximumPhotos() {
        // given
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(ownerId);
        when(mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId)).thenReturn(15L);
        when(mediaProperties.getMaxPhotosPerListing()).thenReturn(15);

        // when + then
        assertThrows(
                PhotoLimitExceededException.class,
                () -> mediaService.uploadPhoto(listingId, file)
        );

        verify(fileValidationService, never()).validate(any());
        verify(storageService, never()).putObject(anyString(), any(), anyString());
        verify(mediaObjectRepository, never()).save(any());
        verify(eventsPublisher, never()).publishPhotoUploaded(any(), any());
    }

    @Test
    void shouldPropagateExceptionWhenFileValidationFails() {
        // given
        RuntimeException exception = new IllegalArgumentException("File is required");

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(ownerId);
        when(mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId)).thenReturn(0L);
        when(mediaProperties.getMaxPhotosPerListing()).thenReturn(15);
        doThrow(exception).when(fileValidationService).validate(file);

        // when + then
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> mediaService.uploadPhoto(listingId, file)
        );

        assertEquals("File is required", thrown.getMessage());
        verify(storageService, never()).putObject(anyString(), any(), anyString());
        verify(mediaObjectRepository, never()).save(any());
        verify(eventsPublisher, never()).publishPhotoUploaded(any(), any());
    }

    @Test
    void shouldCleanupStorageWhenThumbnailCreationFails() throws IOException {
        // given
        byte[] bytes = file.getBytes();
        String sha256 = "sha256-hash";
        String objectKey = "listings/" + listingId + "/photos/" + mediaId + ".jpg";
        String thumbnailKey = "listings/" + listingId + "/thumbs/" + mediaId + ".jpg";

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(ownerId);
        when(mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId)).thenReturn(0L);
        when(mediaProperties.getMaxPhotosPerListing()).thenReturn(15);
        when(hashingService.sha256Hex(bytes)).thenReturn(sha256);
        when(contentTypeMapper.extensionFor("image/jpeg")).thenReturn(".jpg");
        when(objectKeyFactory.photoObjectKey(listingId, mediaId, ".jpg")).thenReturn(objectKey);
        when(objectKeyFactory.thumbnailObjectKey(listingId, mediaId)).thenReturn(thumbnailKey);
        when(imageProcessingService.createJpegThumbnail(bytes))
                .thenThrow(new StorageException("Cannot create thumbnail"));

        try (var mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(mediaId);

            // when + then
            assertThrows(StorageException.class, () -> mediaService.uploadPhoto(listingId, file));

            verify(storageService).putObject(objectKey, bytes, "image/jpeg");
            verify(storageService).deleteObject(objectKey);
            verify(storageService).deleteObject(thumbnailKey);
            verify(mediaObjectRepository, never()).save(any());
            verify(eventsPublisher, never()).publishPhotoUploaded(any(), any());
        }
    }

    @Test
    void shouldCleanupStorageWhenSavingEntityFails() throws IOException {
        // given
        byte[] bytes = file.getBytes();
        byte[] thumbnailBytes = "thumb-bytes".getBytes();
        String sha256 = "sha256-hash";
        String objectKey = "listings/" + listingId + "/photos/" + mediaId + ".jpg";
        String thumbnailKey = "listings/" + listingId + "/thumbs/" + mediaId + ".jpg";

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(listingOwnershipVerifier.requireOwnerOrAdmin(listingId)).thenReturn(ownerId);
        when(mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId)).thenReturn(0L);
        when(mediaProperties.getMaxPhotosPerListing()).thenReturn(15);
        when(hashingService.sha256Hex(bytes)).thenReturn(sha256);
        when(contentTypeMapper.extensionFor("image/jpeg")).thenReturn(".jpg");
        when(objectKeyFactory.photoObjectKey(listingId, mediaId, ".jpg")).thenReturn(objectKey);
        when(objectKeyFactory.thumbnailObjectKey(listingId, mediaId)).thenReturn(thumbnailKey);
        when(imageProcessingService.createJpegThumbnail(bytes)).thenReturn(thumbnailBytes);
        when(s3Properties.getBucket()).thenReturn("estatehub-media");
        when(mediaObjectRepository.save(any(MediaObjectEntity.class)))
                .thenThrow(new RuntimeException("DB save failed"));

        try (var mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(mediaId);

            // when + then
            RuntimeException thrown = assertThrows(
                    RuntimeException.class,
                    () -> mediaService.uploadPhoto(listingId, file)
            );

            assertEquals("DB save failed", thrown.getMessage());
            verify(storageService).putObject(objectKey, bytes, "image/jpeg");
            verify(storageService).putObject(thumbnailKey, thumbnailBytes, "image/jpeg");
            verify(storageService).deleteObject(objectKey);
            verify(storageService).deleteObject(thumbnailKey);
            verify(eventsPublisher, never()).publishPhotoUploaded(any(), any());
        }
    }

    @Test
    void shouldReturnActivePhotosForListingWhenUserCanRead() {
        // given
        MediaObjectEntity photo1 = MediaObjectEntity.builder().id(UUID.randomUUID()).listingId(listingId).build();
        MediaObjectEntity photo2 = MediaObjectEntity.builder().id(UUID.randomUUID()).listingId(listingId).build();

        doNothing().when(listingOwnershipVerifier).requireCanRead(listingId);
        when(mediaObjectRepository.findAllByListingIdAndDeletedAtIsNullOrderByCreatedAtAsc(listingId))
                .thenReturn(List.of(photo1, photo2));

        // when
        List<MediaObjectEntity> result = mediaService.listPhotos(listingId);

        // then
        assertEquals(2, result.size());
        assertEquals(List.of(photo1, photo2), result);
        verify(listingOwnershipVerifier).requireCanRead(listingId);
        verify(mediaObjectRepository).findAllByListingIdAndDeletedAtIsNullOrderByCreatedAtAsc(listingId);
    }

    @Test
    void shouldReturnPhotoWhenExistsAndNotDeleted() {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .build();

        when(mediaObjectRepository.findByIdAndDeletedAtIsNull(mediaId)).thenReturn(Optional.of(entity));

        // when
        MediaObjectEntity result = mediaService.getPhoto(mediaId);

        // then
        assertSame(entity, result);
        verify(mediaObjectRepository).findByIdAndDeletedAtIsNull(mediaId);
    }

    @Test
    void shouldThrowMediaNotFoundExceptionWhenPhotoDoesNotExist() {
        // given
        when(mediaObjectRepository.findByIdAndDeletedAtIsNull(mediaId)).thenReturn(Optional.empty());

        // when + then
        MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> mediaService.getPhoto(mediaId)
        );

        assertEquals("Media not found: " + mediaId, exception.getMessage());
        verify(mediaObjectRepository).findByIdAndDeletedAtIsNull(mediaId);
    }

    @Test
    void shouldReturnPhotoWhenExistsAndUserCanRead() {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .build();

        when(mediaObjectRepository.findByIdAndDeletedAtIsNull(mediaId)).thenReturn(Optional.of(entity));
        doNothing().when(listingOwnershipVerifier).requireCanRead(listingId);

        // when
        MediaObjectEntity result = mediaService.getPhotoForRead(mediaId);

        // then
        assertSame(entity, result);
        verify(mediaObjectRepository).findByIdAndDeletedAtIsNull(mediaId);
        verify(listingOwnershipVerifier).requireCanRead(listingId);
    }

    @Test
    void shouldSoftDeletePhotoDeleteFilesAndPublishEventWhenRequesterIsOwner() {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .bucket("estatehub-media")
                .objectKey("photos/key.jpg")
                .thumbnailKey("thumbs/key.jpg")
                .deletedAt(null)
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.of(entity));
        when(mediaObjectRepository.save(entity)).thenReturn(entity);

        // when
        mediaService.deletePhoto(mediaId);

        // then
        verify(currentUserProvider).requireCurrentUser();
        verify(mediaObjectRepository).findById(mediaId);
        verify(storageService).deleteObject("photos/key.jpg");
        verify(storageService).deleteObject("thumbs/key.jpg");
        verify(mediaObjectRepository).save(entity);

        assertNotNull(entity.getDeletedAt());

        ArgumentCaptor<PhotoDeletedPayload> payloadCaptor = ArgumentCaptor.forClass(PhotoDeletedPayload.class);
        verify(eventsPublisher).publishPhotoDeleted(eq(listingId), payloadCaptor.capture());
        PhotoDeletedPayload payload = payloadCaptor.getValue();

        assertEquals(mediaId, payload.mediaId());
        assertEquals(listingId, payload.listingId());
        assertEquals(ownerId, payload.ownerId());
        assertNotNull(payload.deletedAt());
    }

    @Test
    void shouldSoftDeletePhotoWhenRequesterIsAdminEvenIfNotOwner() {
        // given
        CurrentUser adminUser = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("admin@example.com")
                .roles(Set.of("ADMIN"))
                .build();

        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .objectKey("photos/key.jpg")
                .thumbnailKey("thumbs/key.jpg")
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(adminUser);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.of(entity));
        when(mediaObjectRepository.save(entity)).thenReturn(entity);

        // when
        mediaService.deletePhoto(mediaId);

        // then
        verify(storageService).deleteObject("photos/key.jpg");
        verify(storageService).deleteObject("thumbs/key.jpg");
        verify(mediaObjectRepository).save(entity);
        verify(eventsPublisher).publishPhotoDeleted(eq(listingId), any(PhotoDeletedPayload.class));
        assertNotNull(entity.getDeletedAt());
    }

    @Test
    void shouldThrowMediaNotFoundExceptionWhenDeletingMissingPhoto() {
        // given
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.empty());

        // when + then
        MediaNotFoundException exception = assertThrows(
                MediaNotFoundException.class,
                () -> mediaService.deletePhoto(mediaId)
        );

        assertEquals("Media not found: " + mediaId, exception.getMessage());
        verify(storageService, never()).deleteObject(anyString());
        verify(mediaObjectRepository, never()).save(any());
        verify(eventsPublisher, never()).publishPhotoDeleted(any(), any());
    }

    @Test
    void shouldReturnWithoutDoingAnythingWhenPhotoAlreadyDeleted() {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .objectKey("photos/key.jpg")
                .thumbnailKey("thumbs/key.jpg")
                .deletedAt(Instant.now())
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.of(entity));

        // when
        mediaService.deletePhoto(mediaId);

        // then
        verify(storageService, never()).deleteObject(anyString());
        verify(mediaObjectRepository, never()).save(any());
        verify(eventsPublisher, never()).publishPhotoDeleted(any(), any());
    }

    @Test
    void shouldThrowMediaOwnershipExceptionWhenRequesterIsNotOwnerAndNotAdmin() {
        // given
        CurrentUser stranger = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("stranger@example.com")
                .roles(Set.of("USER"))
                .build();

        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .objectKey("photos/key.jpg")
                .thumbnailKey("thumbs/key.jpg")
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(stranger);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.of(entity));

        // when + then
        MediaOwnershipException exception = assertThrows(
                MediaOwnershipException.class,
                () -> mediaService.deletePhoto(mediaId)
        );

        assertEquals("Access denied for media: " + mediaId, exception.getMessage());
        verify(storageService, never()).deleteObject(anyString());
        verify(mediaObjectRepository, never()).save(any());
        verify(eventsPublisher, never()).publishPhotoDeleted(any(), any());
    }

    @Test
    void shouldStillMarkDeletedWhenStorageCleanupFailsDuringDelete() {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .objectKey("photos/key.jpg")
                .thumbnailKey("thumbs/key.jpg")
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser);
        when(mediaObjectRepository.findById(mediaId)).thenReturn(Optional.of(entity));
        when(mediaObjectRepository.save(entity)).thenReturn(entity);

        doThrow(new StorageException("S3 delete failed"))
                .when(storageService).deleteObject("photos/key.jpg");
        doThrow(new StorageException("S3 delete failed"))
                .when(storageService).deleteObject("thumbs/key.jpg");

        // when
        mediaService.deletePhoto(mediaId);

        // then
        verify(storageService).deleteObject("photos/key.jpg");
        verify(storageService).deleteObject("thumbs/key.jpg");
        verify(mediaObjectRepository).save(entity);
        verify(eventsPublisher).publishPhotoDeleted(eq(listingId), any(PhotoDeletedPayload.class));
        assertNotNull(entity.getDeletedAt());
    }

    @Test
    void shouldReturnPresignedUrlForObjectKey() throws Exception {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .objectKey("photos/key.jpg")
                .build();

        URL expected = new URL("http://localhost:9000/test-object");
        when(storageService.presignedGetUrl("photos/key.jpg")).thenReturn(expected);

        // when
        URL result = mediaService.presignedUrlFor(entity);

        // then
        assertEquals(expected, result);
        verify(storageService).presignedGetUrl("photos/key.jpg");
    }

    @Test
    void shouldReturnPresignedUrlForThumbnailKey() throws Exception {
        // given
        MediaObjectEntity entity = MediaObjectEntity.builder()
                .thumbnailKey("thumbs/key.jpg")
                .build();

        URL expected = new URL("http://localhost:9000/test-thumb");
        when(storageService.presignedGetUrl("thumbs/key.jpg")).thenReturn(expected);

        // when
        URL result = mediaService.presignedThumbnailUrlFor(entity);

        // then
        assertEquals(expected, result);
        verify(storageService).presignedGetUrl("thumbs/key.jpg");
    }
}