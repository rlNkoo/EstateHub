package com.rlnkoo.notificationservice.persistence.repository;

import com.rlnkoo.notificationservice.persistence.entity.UserEmailIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserEmailIndexRepository extends JpaRepository<UserEmailIndexEntity, UUID> {
    Optional<UserEmailIndexEntity> findByUserId(UUID userId);
}