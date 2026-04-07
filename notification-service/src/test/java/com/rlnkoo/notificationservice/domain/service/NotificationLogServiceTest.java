package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.persistence.entity.NotificationLogEntity;
import com.rlnkoo.notificationservice.persistence.entity.NotificationStatus;
import com.rlnkoo.notificationservice.persistence.repository.NotificationLogRepository;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository repository;

    @InjectMocks
    private NotificationLogService notificationLogService;

    @Test
    void shouldReturnTrueAndSaveNotificationLogWhenEventIsReceivedFirstTime() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                occurredAt,
                "payload"
        );

        String recipientEmail = "owner@example.com";

        // when
        boolean result = notificationLogService.tryMarkReceived(envelope, recipientEmail, userId, listingId);

        // then
        assertTrue(result);

        ArgumentCaptor<NotificationLogEntity> entityCaptor = ArgumentCaptor.forClass(NotificationLogEntity.class);
        verify(repository).save(entityCaptor.capture());

        NotificationLogEntity savedEntity = entityCaptor.getValue();
        assertEquals(eventId, savedEntity.getEventId());
        assertEquals("ListingPublishedV1", savedEntity.getEventType());
        assertEquals(occurredAt, savedEntity.getOccurredAt());
        assertEquals(NotificationStatus.RECEIVED, savedEntity.getStatus());
        assertEquals(recipientEmail, savedEntity.getRecipientEmail());
        assertEquals(userId, savedEntity.getUserId());
        assertEquals(listingId, savedEntity.getListingId());
        assertEquals(0, savedEntity.getAttempts());
        assertNull(savedEntity.getLastError());
        assertNotNull(savedEntity.getId());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
        assertNull(savedEntity.getSentAt());
    }

    @Test
    void shouldReturnFalseWhenSavingNotificationLogFailsWithDataIntegrityViolationException() {
        // given
        UUID eventId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                "payload"
        );

        when(repository.save(any(NotificationLogEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when
        boolean result = notificationLogService.tryMarkReceived(
                envelope,
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        // then
        assertFalse(result);
        verify(repository).save(any(NotificationLogEntity.class));
    }

    @Test
    void shouldReturnFalseWhenSavingNotificationLogFailsWithPersistenceException() {
        // given
        UUID eventId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                "payload"
        );

        when(repository.save(any(NotificationLogEntity.class)))
                .thenThrow(new PersistenceException("persistence error"));

        // when
        boolean result = notificationLogService.tryMarkReceived(
                envelope,
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        // then
        assertFalse(result);
        verify(repository).save(any(NotificationLogEntity.class));
    }

    @Test
    void shouldMarkNotificationAsSentWhenLogExists() {
        // given
        UUID eventId = UUID.randomUUID();

        NotificationLogEntity entity = NotificationLogEntity.received(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        when(repository.findByEventId(eventId)).thenReturn(Optional.of(entity));

        // when
        notificationLogService.markSent(eventId);

        // then
        verify(repository).findByEventId(eventId);
        assertEquals(NotificationStatus.SENT, entity.getStatus());
        assertNotNull(entity.getSentAt());
        assertNull(entity.getLastError());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenMarkSentAndNotificationLogDoesNotExist() {
        // given
        UUID eventId = UUID.randomUUID();
        when(repository.findByEventId(eventId)).thenReturn(Optional.empty());

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> notificationLogService.markSent(eventId)
        );

        assertEquals("Notification log not found for eventId=" + eventId, exception.getMessage());
        verify(repository).findByEventId(eventId);
    }

    @Test
    void shouldMarkNotificationAsFailedWhenLogExists() {
        // given
        UUID eventId = UUID.randomUUID();

        NotificationLogEntity entity = NotificationLogEntity.received(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                "user@example.com",
                UUID.randomUUID(),
                null
        );

        RuntimeException exception = new RuntimeException("SMTP connection failed");

        when(repository.findByEventId(eventId)).thenReturn(Optional.of(entity));

        // when
        notificationLogService.markFailed(eventId, exception);

        // then
        verify(repository).findByEventId(eventId);
        assertEquals(NotificationStatus.FAILED, entity.getStatus());
        assertEquals(1, entity.getAttempts());
        assertEquals("SMTP connection failed", entity.getLastError());
        assertNull(entity.getSentAt());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenMarkFailedAndNotificationLogDoesNotExist() {
        // given
        UUID eventId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("mail error");

        when(repository.findByEventId(eventId)).thenReturn(Optional.empty());

        // when + then
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> notificationLogService.markFailed(eventId, exception)
        );

        assertEquals("Notification log not found for eventId=" + eventId, thrown.getMessage());
        verify(repository).findByEventId(eventId);
    }
}