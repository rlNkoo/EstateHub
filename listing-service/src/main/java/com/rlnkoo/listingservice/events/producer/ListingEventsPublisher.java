package com.rlnkoo.listingservice.events.producer;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.commonevents.EventPublisher;
import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.listingservice.events.types.ListingArchivedPayload;
import com.rlnkoo.listingservice.events.types.ListingPublishedPayload;
import com.rlnkoo.listingservice.events.types.ListingUpdatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListingEventsPublisher {

    private final EventPublisher eventPublisher;

    public void publishListingUpdated(UUID listingId, ListingUpdatedPayload payload) {
        EventEnvelope<ListingUpdatedPayload> envelope =
                EventEnvelope.of("ListingUpdatedV1", payload);

        eventPublisher.publish(Topics.LISTING_EVENTS, listingId.toString(), envelope);
    }

    public void publishListingPublished(UUID listingId, ListingPublishedPayload payload) {
        EventEnvelope<ListingPublishedPayload> envelope =
                EventEnvelope.of("ListingPublishedV1", payload);

        eventPublisher.publish(Topics.LISTING_EVENTS, listingId.toString(), envelope);
    }

    public void publishListingArchived(UUID listingId, ListingArchivedPayload payload) {
        EventEnvelope<ListingArchivedPayload> envelope =
                EventEnvelope.of("ListingArchivedV1", payload);

        eventPublisher.publish(Topics.LISTING_EVENTS, listingId.toString(), envelope);
    }
}