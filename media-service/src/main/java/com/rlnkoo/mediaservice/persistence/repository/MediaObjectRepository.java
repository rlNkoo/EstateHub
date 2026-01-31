package com.rlnkoo.mediaservice.persistence.repository;

import com.rlnkoo.mediaservice.persistence.entity.MediaObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaObjectRepository extends JpaRepository<MediaObjectEntity, UUID> {

    long countByListingIdAndDeletedAtIsNull(UUID listingId);

    List<MediaObjectEntity> findAllByListingIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID listingId);

    Optional<MediaObjectEntity> findByIdAndDeletedAtIsNull(UUID id);
}