package com.rlnkoo.listingservice.persistence.repository;

import com.rlnkoo.listingservice.persistence.entity.ListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<ListingEntity, UUID> {

    List<ListingEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}