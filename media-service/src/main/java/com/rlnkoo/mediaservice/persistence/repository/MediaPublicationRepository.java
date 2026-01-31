package com.rlnkoo.mediaservice.persistence.repository;

import com.rlnkoo.mediaservice.persistence.entity.MediaPublicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaPublicationRepository extends JpaRepository<MediaPublicationEntity, UUID> {

    Optional<MediaPublicationEntity> findByListingIdAndMediaId(UUID listingId, UUID mediaId);

    List<MediaPublicationEntity> findAllByListingId(UUID listingId);

    boolean existsByMediaIdAndPublicVisibleTrue(UUID mediaId);

    void deleteAllByListingId(UUID listingId);

    void deleteAllByListingIdAndMediaIdNotIn(UUID listingId, Collection<UUID> mediaIds);
}