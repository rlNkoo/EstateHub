package com.rlnkoo.notificationservice.persistence.repository;

import com.rlnkoo.notificationservice.persistence.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, UUID> {

    boolean existsByEventId(UUID eventId);

    Optional<NotificationLogEntity> findByEventId(UUID eventId);
}