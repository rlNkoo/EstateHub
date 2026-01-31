package com.rlnkoo.mediaservice.persistence.repository;

import com.rlnkoo.mediaservice.domain.model.MediaStatus;
import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaObjectRepository extends JpaRepository<MediaObjectEntity, UUID> {

    List<MediaObjectEntity> findAllByIdIn(Collection<UUID> ids);

    Optional<MediaObjectEntity> findByIdAndStatus(UUID id, MediaStatus status);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}