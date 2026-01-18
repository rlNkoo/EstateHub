package com.rlnkoo.listingservice.persistence.repository;

import com.rlnkoo.listingservice.persistence.entity.ListingVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListingVersionRepository extends JpaRepository<ListingVersionEntity, UUID> {

    Optional<ListingVersionEntity> findByListingIdAndVersionNo(UUID listingId, int versionNo);
}