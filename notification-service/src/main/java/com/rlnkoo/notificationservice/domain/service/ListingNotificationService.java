package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.listing.events.*;
import org.springframework.stereotype.Service;

@Service
public class ListingNotificationService {

    public void onListingPublished(EventEnvelope<?> envelope, ListingPublishedV1Payload payload) {
    }

    public void onListingUpdated(EventEnvelope<?> envelope, ListingUpdatedV1Payload payload) {
    }

    public void onListingArchived(EventEnvelope<?> envelope, ListingArchivedV1Payload payload) {
    }
}