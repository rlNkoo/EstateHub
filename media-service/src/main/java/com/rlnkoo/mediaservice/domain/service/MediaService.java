package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.config.MediaProperties;
import com.rlnkoo.mediaservice.config.S3Properties;
import com.rlnkoo.mediaservice.domain.exception.*;
import com.rlnkoo.mediaservice.events.producer.MediaEventsPublisher;
import com.rlnkoo.mediaservice.events.types.PhotoDeletedPayload;
import com.rlnkoo.mediaservice.events.types.PhotoUploadedPayload;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import com.rlnkoo.mediaservice.persistence.repository.MediaObjectRepository;
import com.rlnkoo.mediaservice.security.CurrentUser;
import com.rlnkoo.mediaservice.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaProperties mediaProperties;
    private final S3Properties s3Properties;

    private final CurrentUserProvider currentUserProvider;
    private final ListingOwnershipVerifier listingOwnershipVerifier;

    private final MediaObjectRepository mediaObjectRepository;

    private final FileValidationService fileValidationService;
    private final HashingService hashingService;
    private final ContentTypeMapper contentTypeMapper;
    private final ObjectKeyFactory objectKeyFactory;
    private final ImageProcessingService imageProcessingService;

    private final S3StorageService storageService;
    private final MediaEventsPublisher eventsPublisher;

    @Transactional
    public MediaObjectEntity uploadPhoto(UUID listingId, MultipartFile file) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("Upload photo request listingId=[{}] requesterId=[{}]", listingId, user.userId());

        UUID ownerId = listingOwnershipVerifier.requireOwnerOrAdmin(listingId);

        long activeCount = mediaObjectRepository.countByListingIdAndDeletedAtIsNull(listingId);
        int max = mediaProperties.getMaxPhotosPerListing();
        if (activeCount >= max) {
            log.warn("Photo limit exceeded listingId=[{}] activeCount=[{}] max=[{}]", listingId, activeCount, max);
            throw new PhotoLimitExceededException(listingId, max);
        }

        fileValidationService.validate(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new StorageException("Cannot read uploaded file bytes", ex);
        }

        String contentType = file.getContentType();
        String sha256 = hashingService.sha256Hex(bytes);

        UUID mediaId = UUID.randomUUID();
        String extension = contentTypeMapper.extensionFor(contentType);

        String objectKey = objectKeyFactory.photoObjectKey(listingId, mediaId, extension);

        String thumbnailKey = objectKeyFactory.thumbnailObjectKey(listingId, mediaId);

        storageService.putObject(objectKey, bytes, contentType);

        byte[] thumbBytes = imageProcessingService.createJpegThumbnail(bytes);
        storageService.putObject(thumbnailKey, thumbBytes, "image/jpeg");

        MediaObjectEntity entity = MediaObjectEntity.builder()
                .id(mediaId)
                .listingId(listingId)
                .ownerId(ownerId)
                .bucket(s3Properties.getBucket())
                .objectKey(objectKey)
                .thumbnailKey(thumbnailKey)
                .contentType(contentType)
                .sizeBytes(bytes.length)
                .sha256(sha256)
                .createdAt(Instant.now())
                .build();

        try {
            mediaObjectRepository.save(entity);
        } catch (Exception ex) {
            try {
                storageService.deleteObject(objectKey);
            } catch (Exception cleanupEx) {
                log.warn("Cleanup failed for objectKey=[{}] message=[{}]", objectKey, cleanupEx.getMessage());
            }
            try {
                storageService.deleteObject(thumbnailKey);
            } catch (Exception cleanupEx) {
                log.warn("Cleanup failed for thumbnailKey=[{}] message=[{}]", thumbnailKey, cleanupEx.getMessage());
            }
            throw ex;
        }

        eventsPublisher.publishPhotoUploaded(
                listingId,
                PhotoUploadedPayload.builder()
                        .mediaId(entity.getId())
                        .listingId(entity.getListingId())
                        .ownerId(entity.getOwnerId())
                        .bucket(entity.getBucket())
                        .objectKey(entity.getObjectKey())
                        .thumbnailKey(entity.getThumbnailKey())
                        .contentType(entity.getContentType())
                        .sizeBytes(entity.getSizeBytes())
                        .sha256(entity.getSha256())
                        .uploadedAt(entity.getCreatedAt())
                        .build()
        );

        log.info("Photo uploaded listingId=[{}] mediaId=[{}] ownerId=[{}]", listingId, mediaId, ownerId);
        return entity;
    }

    @Transactional(readOnly = true)
    public List<MediaObjectEntity> listPhotos(UUID listingId) {
        log.debug("List photos listingId=[{}]", listingId);
        return mediaObjectRepository.findAllByListingIdAndDeletedAtIsNullOrderByCreatedAtAsc(listingId);
    }

    @Transactional(readOnly = true)
    public MediaObjectEntity getPhoto(UUID mediaId) {
        return mediaObjectRepository.findByIdAndDeletedAtIsNull(mediaId)
                .orElseThrow(() -> new MediaNotFoundException(mediaId));
    }

    @Transactional
    public void deletePhoto(UUID mediaId) {
        CurrentUser user = currentUserProvider.requireCurrentUser();
        log.info("Delete photo request mediaId=[{}] requesterId=[{}]", mediaId, user.userId());

        MediaObjectEntity entity = mediaObjectRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException(mediaId));

        if (entity.isDeleted()) {
            log.info("Delete photo skipped: already deleted mediaId=[{}]", mediaId);
            return;
        }

        boolean isAdmin = user.roles().contains("ADMIN");
        boolean isOwner = user.userId().equals(entity.getOwnerId());
        if (!isAdmin && !isOwner) {
            log.warn("Delete denied mediaId=[{}] ownerId=[{}] requesterId=[{}] roles=[{}]",
                    mediaId, entity.getOwnerId(), user.userId(), user.roles());
            throw new MediaOwnershipException(mediaId);
        }

        storageService.deleteObject(entity.getObjectKey());
        if (entity.getThumbnailKey() != null && !entity.getThumbnailKey().isBlank()) {
            storageService.deleteObject(entity.getThumbnailKey());
        }

        entity.markDeleted();
        mediaObjectRepository.save(entity);

        eventsPublisher.publishPhotoDeleted(
                entity.getListingId(),
                PhotoDeletedPayload.builder()
                        .mediaId(entity.getId())
                        .listingId(entity.getListingId())
                        .ownerId(entity.getOwnerId())
                        .deletedAt(entity.getDeletedAt())
                        .build()
        );

        log.info("Photo deleted mediaId=[{}] listingId=[{}]", mediaId, entity.getListingId());
    }

    @Transactional(readOnly = true)
    public URL getPresignedUrl(UUID mediaId) {
        MediaObjectEntity entity = getPhoto(mediaId);
        return storageService.presignedGetUrl(entity.getObjectKey());
    }

    @Transactional(readOnly = true)
    public URL getPresignedThumbnailUrl(UUID mediaId) {
        MediaObjectEntity entity = getPhoto(mediaId);
        return storageService.presignedGetUrl(entity.getThumbnailKey());
    }
}