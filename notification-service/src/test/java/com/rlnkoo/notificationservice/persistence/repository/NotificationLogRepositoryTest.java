package com.rlnkoo.notificationservice.persistence.repository;

import com.rlnkoo.notificationservice.persistence.entity.NotificationLogEntity;
import com.rlnkoo.notificationservice.persistence.entity.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NotificationLogRepositoryTest {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Test
    void shouldSaveNotificationLogAndFindItByEventId() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2025-01-10T12:30:00Z");

        NotificationLogEntity entity = NotificationLogEntity.received(
                eventId,
                "ListingPublishedV1",
                occurredAt,
                "owner@example.com",
                userId,
                listingId
        );

        // when
        notificationLogRepository.saveAndFlush(entity);

        // then
        NotificationLogEntity saved = notificationLogRepository.findByEventId(eventId).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(eventId, saved.getEventId());
        assertEquals("ListingPublishedV1", saved.getEventType());
        assertEquals(occurredAt, saved.getOccurredAt());
        assertEquals(NotificationStatus.RECEIVED, saved.getStatus());
        assertEquals("owner@example.com", saved.getRecipientEmail());
        assertEquals(userId, saved.getUserId());
        assertEquals(listingId, saved.getListingId());
        assertEquals(0, saved.getAttempts());
        assertNull(saved.getLastError());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNull(saved.getSentAt());
    }

    @Test
    void shouldEnforceUniqueConstraintForEventId() {
        // given
        UUID eventId = UUID.randomUUID();

        NotificationLogEntity first = NotificationLogEntity.received(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        NotificationLogEntity second = NotificationLogEntity.received(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        notificationLogRepository.saveAndFlush(first);

        // when + then
        assertThrows(
                DataIntegrityViolationException.class,
                () -> notificationLogRepository.saveAndFlush(second)
        );
    }
}