package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.persistence.entity.NotificationLogEntity;
import com.rlnkoo.notificationservice.persistence.repository.NotificationLogRepository;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository repository;

    @Transactional
    public boolean tryMarkReceived(EventEnvelope<?> envelope, String recipientEmail, UUID userId, UUID listingId) {
        try {
            var entity = NotificationLogEntity.received(
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.occurredAt(),
                    recipientEmail,
                    userId,
                    listingId
            );
            repository.save(entity);
            return true;

        } catch (DataIntegrityViolationException | PersistenceException ex) {
            return false;
        }
    }

    @Transactional
    public void markSent(UUID eventId) {
        var entity = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Notification log not found for eventId=" + eventId));
        entity.markSent();
    }

    @Transactional
    public void markFailed(UUID eventId, Exception ex) {
        var entity = repository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Notification log not found for eventId=" + eventId));
        entity.markFailed(ex.getMessage());
    }
}