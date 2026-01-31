package com.rlnkoo.mediaservice.domain.service;

import com.rlnkoo.mediaservice.domain.exception.MediaAccessDeniedException;
import com.rlnkoo.mediaservice.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OwnershipPolicy {

    public void assertOwnerOrAdmin(CurrentUser user, UUID ownerId, UUID mediaId) {
        boolean isOwner = user.userId().equals(ownerId);
        boolean isAdmin = user.roles() != null && user.roles().contains("ADMIN");

        if (!isOwner && !isAdmin) {
            log.warn("Access denied mediaId=[{}] ownerId=[{}] requesterId=[{}] roles=[{}]",
                    mediaId, ownerId, user.userId(), user.roles());
            throw new MediaAccessDeniedException(mediaId);
        }
    }

    public boolean isAdmin(CurrentUser user) {
        return user.roles() != null && user.roles().contains("ADMIN");
    }
}