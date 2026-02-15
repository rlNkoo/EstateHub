package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.listing.events.*;
import com.rlnkoo.notificationservice.mail.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingNotificationService {

    private final NotificationLogService logService;
    private final NotificationEmailService emailService;
    private final UserEmailIndexService userEmailIndexService;

    @Value("${notification.app.base-url}")
    private String baseUrl;

    public void onListingPublished(EventEnvelope<?> envelope, ListingPublishedV1Payload payload) {
        String email = userEmailIndexService.requireEmail(payload.ownerId());

        boolean firstTime = logService.tryMarkReceived(envelope, email, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        try {
            String listingLink = baseUrl + "/listings/" + payload.listingId();

            var model = new HashMap<String, Object>();
            model.put("email", email);
            model.put("listingId", payload.listingId());
            model.put("title", payload.title());
            model.put("publishedAt", payload.publishedAt());
            model.put("listingLink", listingLink);

            var msg = new EmailMessage(
                    email,
                    EmailSubjects.LISTING_PUBLISHED,
                    EmailTemplates.LISTING_PUBLISHED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send ListingPublished email. eventId=[{}], listingId=[{}], ownerId=[{}]",
                    envelope.eventId(), payload.listingId(), payload.ownerId(), ex);
            throw ex;
        }
    }

    public void onListingUpdated(EventEnvelope<?> envelope, ListingUpdatedV1Payload payload) {
        String email = userEmailIndexService.requireEmail(payload.ownerId());

        boolean firstTime = logService.tryMarkReceived(envelope, email, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        try {
            String listingLink = baseUrl + "/listings/" + payload.listingId();

            var model = new HashMap<String, Object>();
            model.put("email", email);
            model.put("listingId", payload.listingId());
            model.put("title", payload.title());
            model.put("version", payload.version());
            model.put("listingLink", listingLink);

            var msg = new EmailMessage(
                    email,
                    EmailSubjects.LISTING_UPDATED,
                    EmailTemplates.LISTING_UPDATED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send ListingUpdated email. eventId=[{}], listingId=[{}], ownerId=[{}]",
                    envelope.eventId(), payload.listingId(), payload.ownerId(), ex);
            throw ex;
        }
    }

    public void onListingArchived(EventEnvelope<?> envelope, ListingArchivedV1Payload payload) {
        String email = userEmailIndexService.requireEmail(payload.ownerId());

        boolean firstTime = logService.tryMarkReceived(envelope, email, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        try {
            var model = new HashMap<String, Object>();
            model.put("email", email);
            model.put("listingId", payload.listingId());
            model.put("archivedAt", payload.archivedAt());

            var msg = new EmailMessage(
                    email,
                    EmailSubjects.LISTING_ARCHIVED,
                    EmailTemplates.LISTING_ARCHIVED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send ListingArchived email. eventId=[{}], listingId=[{}], ownerId=[{}]",
                    envelope.eventId(), payload.listingId(), payload.ownerId(), ex);
            throw ex;
        }
    }

    private void logDuplicate(EventEnvelope<?> envelope) {
        log.info("Duplicate event ignored. eventId=[{}], eventType=[{}]", envelope.eventId(), envelope.eventType());
    }
}