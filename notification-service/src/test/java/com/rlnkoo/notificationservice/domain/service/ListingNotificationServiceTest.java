package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.listing.events.ListingArchivedV1Payload;
import com.rlnkoo.notificationservice.integration.listing.events.ListingPublishedV1Payload;
import com.rlnkoo.notificationservice.integration.listing.events.ListingUpdatedV1Payload;
import com.rlnkoo.notificationservice.mail.EmailMessage;
import com.rlnkoo.notificationservice.mail.EmailSubjects;
import com.rlnkoo.notificationservice.mail.EmailTemplates;
import com.rlnkoo.notificationservice.mail.NotificationEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingNotificationServiceTest {

    @Mock
    private NotificationLogService logService;

    @Mock
    private NotificationEmailService emailService;

    @Mock
    private UserEmailIndexService userEmailIndexService;

    @InjectMocks
    private ListingNotificationService listingNotificationService;

    @BeforeEach
    void setUp() {
        setField(listingNotificationService, "baseUrl", "http://localhost:8085");
    }

    @Test
    void shouldRequireOwnerEmailMarkReceivedSendEmailAndMarkSentWhenListingPublishedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant publishedAt = Instant.now();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                "payload"
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                publishedAt,
                "Beautiful apartment"
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);

        // when
        listingNotificationService.onListingPublished(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals(email, message.to());
        assertEquals(EmailSubjects.LISTING_PUBLISHED, message.subject());
        assertEquals(EmailTemplates.LISTING_PUBLISHED, message.template());
        assertEquals(email, message.model().get("email"));
        assertEquals(listingId, message.model().get("listingId"));
        assertEquals("Beautiful apartment", message.model().get("title"));
        assertEquals(publishedAt, message.model().get("publishedAt"));
        assertEquals("http://localhost:8085/listings/" + listingId, message.model().get("listingLink"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicateListingPublishedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                "payload"
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                Instant.now(),
                "Beautiful apartment"
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(false);

        // when
        listingNotificationService.onListingPublished(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingListingPublishedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                "payload"
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                Instant.now(),
                "Beautiful apartment"
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listingNotificationService.onListingPublished(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);
        verify(emailService).send(any(EmailMessage.class));
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldPropagateExceptionWhenOwnerEmailDoesNotExistForListingPublishedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingPublishedV1",
                Instant.now(),
                "payload"
        );

        ListingPublishedV1Payload payload = new ListingPublishedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                1,
                Instant.now(),
                "Beautiful apartment"
        );

        IllegalStateException exception = new IllegalStateException("Email not found for userId=" + ownerId);
        when(userEmailIndexService.requireEmail(ownerId)).thenThrow(exception);

        // when + then
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> listingNotificationService.onListingPublished(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService, never()).tryMarkReceived(any(), any(), any(), any());
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldRequireOwnerEmailMarkReceivedSendEmailAndMarkSentWhenListingUpdatedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                "payload"
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);

        // when
        listingNotificationService.onListingUpdated(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals(email, message.to());
        assertEquals(EmailSubjects.LISTING_UPDATED, message.subject());
        assertEquals(EmailTemplates.LISTING_UPDATED, message.template());
        assertEquals(email, message.model().get("email"));
        assertEquals(listingId, message.model().get("listingId"));
        assertEquals("Updated apartment", message.model().get("title"));
        assertEquals(2, message.model().get("version"));
        assertEquals("http://localhost:8085/listings/" + listingId, message.model().get("listingLink"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicateListingUpdatedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                "payload"
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(false);

        // when
        listingNotificationService.onListingUpdated(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingListingUpdatedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                "payload"
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listingNotificationService.onListingUpdated(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldPropagateExceptionWhenOwnerEmailDoesNotExistForListingUpdatedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingUpdatedV1",
                Instant.now(),
                "payload"
        );

        ListingUpdatedV1Payload payload = new ListingUpdatedV1Payload(
                listingId,
                ownerId,
                "PUBLISHED",
                2,
                "Updated apartment"
        );

        IllegalStateException exception = new IllegalStateException("Email not found for userId=" + ownerId);
        when(userEmailIndexService.requireEmail(ownerId)).thenThrow(exception);

        // when + then
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> listingNotificationService.onListingUpdated(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService, never()).tryMarkReceived(any(), any(), any(), any());
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldRequireOwnerEmailMarkReceivedSendEmailAndMarkSentWhenListingArchivedEventIsHandled() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant archivedAt = Instant.now();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                "payload"
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                archivedAt
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);

        // when
        listingNotificationService.onListingArchived(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals(email, message.to());
        assertEquals(EmailSubjects.LISTING_ARCHIVED, message.subject());
        assertEquals(EmailTemplates.LISTING_ARCHIVED, message.template());
        assertEquals(email, message.model().get("email"));
        assertEquals(listingId, message.model().get("listingId"));
        assertEquals(archivedAt, message.model().get("archivedAt"));

        verify(logService).markSent(eventId);
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldIgnoreDuplicateListingArchivedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                "payload"
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                Instant.now()
        );

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(false);

        // when
        listingNotificationService.onListingArchived(envelope, payload);

        // then
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService).tryMarkReceived(envelope, email, ownerId, listingId);
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    @Test
    void shouldMarkNotificationAsFailedAndRethrowExceptionWhenSendingListingArchivedEmailFails() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String email = "owner@example.com";

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                "payload"
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                Instant.now()
        );

        RuntimeException exception = new RuntimeException("SMTP failed");

        when(userEmailIndexService.requireEmail(ownerId)).thenReturn(email);
        when(logService.tryMarkReceived(envelope, email, ownerId, listingId)).thenReturn(true);
        doThrow(exception).when(emailService).send(any(EmailMessage.class));

        // when + then
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> listingNotificationService.onListingArchived(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(logService).markFailed(eventId, exception);
        verify(logService, never()).markSent(any());
    }

    @Test
    void shouldPropagateExceptionWhenOwnerEmailDoesNotExistForListingArchivedEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        EventEnvelope<String> envelope = new EventEnvelope<>(
                eventId,
                "ListingArchivedV1",
                Instant.now(),
                "payload"
        );

        ListingArchivedV1Payload payload = new ListingArchivedV1Payload(
                listingId,
                ownerId,
                "ARCHIVED",
                3,
                Instant.now()
        );

        IllegalStateException exception = new IllegalStateException("Email not found for userId=" + ownerId);
        when(userEmailIndexService.requireEmail(ownerId)).thenThrow(exception);

        // when + then
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> listingNotificationService.onListingArchived(envelope, payload)
        );

        assertSame(exception, thrown);
        verify(userEmailIndexService).requireEmail(ownerId);
        verify(logService, never()).tryMarkReceived(any(), any(), any(), any());
        verify(emailService, never()).send(any());
        verify(logService, never()).markSent(any());
        verify(logService, never()).markFailed(any(), any());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}