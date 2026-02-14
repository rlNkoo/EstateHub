package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.listing.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingNotificationService {

    private final NotificationLogService logService;

    public void onListingPublished(EventEnvelope<?> envelope, ListingPublishedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, null, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("ListingPublished received. eventId={}, listingId={}, ownerId={}, title={}",
                envelope.eventId(), payload.listingId(), payload.ownerId(), payload.title());
    }

    public void onListingUpdated(EventEnvelope<?> envelope, ListingUpdatedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, null, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("ListingUpdated received. eventId={}, listingId={}, ownerId={}, title={}",
                envelope.eventId(), payload.listingId(), payload.ownerId(), payload.title());
    }

    public void onListingArchived(EventEnvelope<?> envelope, ListingArchivedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, null, payload.ownerId(), payload.listingId());
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("ListingArchived received. eventId={}, listingId={}, ownerId={}, archivedAt={}",
                envelope.eventId(), payload.listingId(), payload.ownerId(), payload.archivedAt());
    }

    private void logDuplicate(EventEnvelope<?> envelope) {
        log.info("Duplicate event ignored. eventId={}, eventType={}", envelope.eventId(), envelope.eventType());
    }
}