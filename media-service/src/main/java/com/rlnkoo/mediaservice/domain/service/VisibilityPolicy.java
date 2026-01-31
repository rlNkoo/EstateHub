package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.MediaAccessDeniedException;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import com.rlnkoo.mediaservice.persistence.repository.MediaPublicationRepository;
import com.rlnkoo.mediaservice.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisibilityPolicy {

    private final MediaPublicationRepository publicationRepository;

    public void assertCanRead(Optional<CurrentUser> userOpt, MediaObjectEntity media) {
        UUID mediaId = media.getId();

        boolean isPublic = publicationRepository.existsByMediaIdAndPublicVisibleTrue(mediaId);
        if (isPublic) {
            return;
        }

        if (userOpt.isEmpty()) {
            log.warn("Anonymous access denied mediaId=[{}]", mediaId);
            throw new MediaAccessDeniedException(mediaId);
        }

        CurrentUser user = userOpt.get();
        boolean isOwner = user.userId().equals(media.getOwnerId());
        boolean isAdmin = user.roles() != null && user.roles().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            log.warn("Read access denied mediaId=[{}] ownerId=[{}] requesterId=[{}] roles=[{}]",
                    mediaId, media.getOwnerId(), user.userId(), user.roles());
            throw new MediaAccessDeniedException(mediaId);
        }
    }
}