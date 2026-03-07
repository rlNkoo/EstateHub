package com.rlnkoo.searchservice.integration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.searchservice.domain.service.ListingIndexingService;
import com.rlnkoo.searchservice.integration.kafka.events.ListingArchivedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingPublishedV1Payload;
import com.rlnkoo.searchservice.integration.kafka.events.ListingUpdatedV1Payload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingEventsConsumer {

    private final ObjectMapper objectMapper;
    private final ListingIndexingService indexingService;

    @KafkaListener(
            topics = Topics.LISTING_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText(null);
            JsonNode payloadNode = root.path("payload");

            if (eventType == null) {
                log.warn("Skipping listing-event without eventType: [{}]", message);
                return;
            }

            switch (eventType) {
                case "ListingPublishedV1" -> {
                    ListingPublishedV1Payload payload =
                            objectMapper.treeToValue(payloadNode, ListingPublishedV1Payload.class);

                    if (payload.listingId() == null || payload.ownerId() == null) {
                        log.warn("Skipping ListingPublishedV1 with missing ids: [{}]", payloadNode);
                        return;
                    }

                    indexingService.onListingPublished(payload);
                }
                case "ListingUpdatedV1" -> {
                    ListingUpdatedV1Payload payload =
                            objectMapper.treeToValue(payloadNode, ListingUpdatedV1Payload.class);

                    if (payload.listingId() == null || payload.ownerId() == null) {
                        log.warn("Skipping ListingUpdatedV1 with missing ids: [{}]", payloadNode);
                        return;
                    }

                    indexingService.onListingUpdated(payload);
                }
                case "ListingArchivedV1" -> {
                    ListingArchivedV1Payload payload =
                            objectMapper.treeToValue(payloadNode, ListingArchivedV1Payload.class);

                    if (payload.listingId() == null || payload.ownerId() == null) {
                        log.warn("Skipping ListingArchivedV1 with missing ids: [{}]", payloadNode);
                        return;
                    }

                    indexingService.onListingArchived(payload);
                }
                default -> log.debug("Ignoring eventType=[{}]", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process listing-event: [{}]", message, ex);
            throw new RuntimeException(ex);
        }
    }
}