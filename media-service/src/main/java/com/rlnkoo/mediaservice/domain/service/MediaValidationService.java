package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.api.internal.dto.PhotoValidationError;
import com.rlnkoo.mediaservice.api.internal.dto.ValidatePhotoOwnershipRequest;
import com.rlnkoo.mediaservice.api.internal.dto.ValidatePhotoOwnershipResponse;
import com.rlnkoo.mediaservice.domain.model.MediaStatus;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import com.rlnkoo.mediaservice.persistence.repository.MediaObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private final MediaObjectRepository mediaObjectRepository;

    @Transactional(readOnly = true)
    public ValidatePhotoOwnershipResponse validate(ValidatePhotoOwnershipRequest request) {
        UUID requesterId = request.requesterId();
        Set<String> roles = request.requesterRoles() == null ? Set.of() : request.requesterRoles();
        boolean isAdmin = roles.contains("ADMIN");

        List<UUID> photoIds = request.photoIds() == null ? List.of() : request.photoIds();
        if (photoIds.isEmpty()) {
            return ValidatePhotoOwnershipResponse.builder()
                    .valid(true)
                    .errors(List.of())
                    .build();
        }

        log.info("Validate photo ownership requesterId=[{}] isAdmin=[{}] photoIdsCount=[{}]",
                requesterId, isAdmin, photoIds.size());

        List<MediaObjectEntity> found = mediaObjectRepository.findAllByIdIn(photoIds);
        Map<UUID, MediaObjectEntity> foundMap = new HashMap<>();
        for (MediaObjectEntity entity : found) {
            foundMap.put(entity.getId(), entity);
        }

        List<PhotoValidationError> errors = new ArrayList<>();

        for (UUID mediaId : photoIds) {
            MediaObjectEntity media = foundMap.get(mediaId);

            if (media == null) {
                errors.add(PhotoValidationError.builder()
                        .mediaId(mediaId)
                        .code("NOT_FOUND")
                        .message("Media not found")
                        .build());
                continue;
            }

            MediaStatus status = media.getStatus();
            if (status != MediaStatus.READY) {
                errors.add(PhotoValidationError.builder()
                        .mediaId(mediaId)
                        .code("NOT_READY")
                        .message("Media is not READY (status=" + status + ")")
                        .build());
                continue;
            }

            boolean isOwner = requesterId != null && requesterId.equals(media.getOwnerId());
            if (!isOwner && !isAdmin) {
                errors.add(PhotoValidationError.builder()
                        .mediaId(mediaId)
                        .code("NOT_OWNER")
                        .message("Requester is not owner of media")
                        .build());
            }
        }

        boolean valid = errors.isEmpty();

        if (valid) {
            log.info("Photo ownership validation OK requesterId=[{}] photoIdsCount=[{}]", requesterId, photoIds.size());
        } else {
            log.warn("Photo ownership validation FAILED requesterId=[{}] errorsCount=[{}]", requesterId, errors.size());
        }

        return ValidatePhotoOwnershipResponse.builder()
                .valid(valid)
                .errors(List.copyOf(errors))
                .build();
    }
}