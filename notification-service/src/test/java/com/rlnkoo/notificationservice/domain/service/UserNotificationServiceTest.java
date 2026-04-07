package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.user.events.PasswordResetCompletedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.PasswordResetRequestedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.UserActivatedV1Payload;
import com.rlnkoo.notificationservice.integration.user.events.UserRegisteredV1Payload;
import com.rlnkoo.notificationservice.mail.EmailMessage;
import com.rlnkoo.notificationservice.mail.EmailSubjects;
import com.rlnkoo.notificationservice.mail.EmailTemplates;
import com.rlnkoo.notificationservice.mail.NotificationEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private NotificationLogService logService;

    @Mock
    private NotificationEmailService emailService;

    @Mock
    private UserEmailIndexService userEmailIndexService;

    @InjectMocks
    private UserNotificationService userNotificationService;

    @Test
    void shouldUpsertEmailMarkReceivedSendEmailAndMarkSentWhenUserRegisteredEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                occurredAt,
                "payload"
        );

        UserRegisteredV1Payload payload = new UserRegisteredV1Payload(
                userId,
                "user@example.com",
                "activation-token-123"
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);

        // when
        userNotificationService.onUserRegistered(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("user@example.com", message.to());
        assertEquals(EmailSubjects.USER_REGISTERED, message.subject());
        assertEquals(EmailTemplates.USER_REGISTERED, message.template());
        assertEquals("user@example.com", message.model().get("email"));
        assertEquals("activation-token-123", message.model().get("activationToken"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicateUserRegisteredEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                "payload"
        );

        UserRegisteredV1Payload payload = new UserRegisteredV1Payload(
                userId,
                "user@example.com",
                "activation-token-123"
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(false);

        // when
        userNotificationService.onUserRegistered(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingUserRegisteredEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserRegisteredV1",
                Instant.now(),
                "payload"
        );

        UserRegisteredV1Payload payload = new UserRegisteredV1Payload(
                userId,
                "user@example.com",
                "activation-token-123"
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userNotificationService.onUserRegistered(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);
        verify(emailService).send(any(EmailMessage.class));
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldUpsertEmailMarkReceivedSendEmailAndMarkSentWhenUserActivatedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant activatedAt = Instant.now();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                "payload"
        );

        UserActivatedV1Payload payload = new UserActivatedV1Payload(
                userId,
                "user@example.com",
                activatedAt
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);

        // when
        userNotificationService.onUserActivated(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("user@example.com", message.to());
        assertEquals(EmailSubjects.USER_ACTIVATED, message.subject());
        assertEquals(EmailTemplates.USER_ACTIVATED, message.template());
        assertEquals("user@example.com", message.model().get("email"));
        assertEquals(activatedAt, message.model().get("activatedAt"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicateUserActivatedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                "payload"
        );

        UserActivatedV1Payload payload = new UserActivatedV1Payload(
                userId,
                "user@example.com",
                Instant.now()
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(false);

        // when
        userNotificationService.onUserActivated(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingUserActivatedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "UserActivatedV1",
                Instant.now(),
                "payload"
        );

        UserActivatedV1Payload payload = new UserActivatedV1Payload(
                userId,
                "user@example.com",
                Instant.now()
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userNotificationService.onUserActivated(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldUpsertEmailMarkReceivedSendEmailAndMarkSentWhenPasswordResetRequestedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetRequestedV1Payload payload = new PasswordResetRequestedV1Payload(
                userId,
                "user@example.com",
                "reset-token-123"
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);

        // when
        userNotificationService.onPasswordResetRequested(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("user@example.com", message.to());
        assertEquals(EmailSubjects.PASSWORD_RESET_REQUESTED, message.subject());
        assertEquals(EmailTemplates.PASSWORD_RESET_REQUESTED, message.template());
        assertEquals("user@example.com", message.model().get("email"));
        assertEquals("reset-token-123", message.model().get("resetToken"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicatePasswordResetRequestedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetRequestedV1Payload payload = new PasswordResetRequestedV1Payload(
                userId,
                "user@example.com",
                "reset-token-123"
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(false);

        // when
        userNotificationService.onPasswordResetRequested(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingPasswordResetRequestedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetRequestedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetRequestedV1Payload payload = new PasswordResetRequestedV1Payload(
                userId,
                "user@example.com",
                "reset-token-123"
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userNotificationService.onPasswordResetRequested(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldUpsertEmailMarkReceivedSendEmailAndMarkSentWhenPasswordResetCompletedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant completedAt = Instant.now();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetCompletedV1Payload payload = new PasswordResetCompletedV1Payload(
                userId,
                "user@example.com",
                completedAt
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);

        // when
        userNotificationService.onPasswordResetCompleted(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("user@example.com", message.to());
        assertEquals(EmailSubjects.PASSWORD_RESET_COMPLETED, message.subject());
        assertEquals(EmailTemplates.PASSWORD_RESET_COMPLETED, message.template());
        assertEquals("user@example.com", message.model().get("email"));
        assertEquals(completedAt, message.model().get("completedAt"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicatePasswordResetCompletedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetCompletedV1Payload payload = new PasswordResetCompletedV1Payload(
                userId,
                "user@example.com",
                Instant.now()
        );

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(false);

        // when
        userNotificationService.onPasswordResetCompleted(envelope, payload);

        // then
        verify(userEmailIndexService).upsert(userId, "user@example.com");
        verify(logService).tryMarkReceived(envelope, "user@example.com", userId, null);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingPasswordResetCompletedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "PasswordResetCompletedV1",
                Instant.now(),
                "payload"
        );

        PasswordResetCompletedV1Payload payload = new PasswordResetCompletedV1Payload(
                userId,
                "user@example.com",
                Instant.now()
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(logService.tryMarkReceived(envelope, "user@example.com", userId, null)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> userNotificationService.onPasswordResetCompleted(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }
}