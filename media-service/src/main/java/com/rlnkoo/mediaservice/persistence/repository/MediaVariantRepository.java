package com.rlnkoo.mediaservice.persistence.repository;

import com.rlnkoo.mediaservice.persistence.entity.MediaVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaVariantRepository extends JpaRepository<MediaVariantEntity, UUID> {

    List<MediaVariantEntity> findAllByMediaId(UUID mediaId);

    Optional<MediaVariantEntity> findByMediaIdAndVariantType(UUID mediaId, String variantType);
}