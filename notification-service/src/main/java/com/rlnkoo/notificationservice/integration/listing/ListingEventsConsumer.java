package com.rlnkoo.notificationservice.integration.listing;

import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.notificationservice.domain.service.ListingNotificationService;
import com.rlnkoo.notificationservice.integration.kafka.EventEnvelopeReader;
import com.rlnkoo.notificationservice.integration.listing.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingEventsConsumer {

    private final EventEnvelopeReader reader;
    private final ListingNotificationService listingNotificationService;

    @KafkaListener(topics = Topics.LISTING_EVENTS, groupId = "notification-service-listing")
    public void onMessage(String message) {
        try {
            var envelope = reader.read(message);

            if (envelope.eventId() == null || envelope.eventType() == null || envelope.payload() == null) {
                log.warn("Invalid listing envelope: missing eventId/eventType/payload. raw=[{}]", message);
                return;
            }

            switch (envelope.eventType()) {
                case "ListingPublishedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), ListingPublishedV1Payload.class);
                    if (payload.listingId() == null || payload.ownerId() == null || payload.publishedAt() == null) {
                        log.warn("ListingPublishedV1 missing required fields. eventId=[{}], payload=[{}]",
                                envelope.eventId(), payload);
                        return;
                    }
                    listingNotificationService.onListingPublished(envelope, payload);
                }
                case "ListingUpdatedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), ListingUpdatedV1Payload.class);
                    if (payload.listingId() == null || payload.ownerId() == null) {
                        log.warn("ListingUpdatedV1 missing required fields. eventId=[{}], payload=[{}]",
                                envelope.eventId(), payload);
                        return;
                    }
                    listingNotificationService.onListingUpdated(envelope, payload);
                }
                case "ListingArchivedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), ListingArchivedV1Payload.class);
                    if (payload.listingId() == null || payload.ownerId() == null || payload.archivedAt() == null) {
                        log.warn("ListingArchivedV1 missing required fields. eventId=[{}], payload=[{}]",
                                envelope.eventId(), payload);
                        return;
                    }
                    listingNotificationService.onListingArchived(envelope, payload);
                }
                default -> log.debug("Ignoring unknown listing eventType=[{}]", envelope.eventType());
            }

        } catch (Exception ex) {
            log.error("Cannot process listing event message. raw=[{}]", message, ex);
            throw ex;
        }
    }
}