package com.rlnkoo.listingservice.integration.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.listingservice.domain.service.ListingMediaSyncService;
import com.rlnkoo.listingservice.integration.media.events.PhotoDeletedV1Payload;
import com.rlnkoo.listingservice.integration.media.events.PhotoUploadedV1Payload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaEventsConsumer {

    private final ObjectMapper objectMapper;
    private final ListingMediaSyncService syncService;

    @KafkaListener(
            topics = "media-events",
            groupId = "listing-service-media-sync"
    )
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText(null);
            JsonNode payloadNode = root.path("payload");

            if (eventType == null) {
                log.warn("Skipping media-event without eventType: [{}]", message);
                return;
            }

            switch (eventType) {
                case "PhotoUploadedV1" -> {
                    PhotoUploadedV1Payload payload =
                            objectMapper.treeToValue(payloadNode, PhotoUploadedV1Payload.class);
                    if (payload.listingId() == null || payload.mediaId() == null) {
                        log.warn("Skipping PhotoUploadedV1 with missing ids: [{}]", payloadNode);
                        return;
                    }
                    syncService.onPhotoUploaded(payload.listingId(), payload.mediaId());
                }
                case "PhotoDeletedV1" -> {
                    PhotoDeletedV1Payload payload =
                            objectMapper.treeToValue(payloadNode, PhotoDeletedV1Payload.class);
                    if (payload.listingId() == null || payload.mediaId() == null) {
                        log.warn("Skipping PhotoDeletedV1 with missing ids: [{}]", payloadNode);
                        return;
                    }
                    syncService.onPhotoDeleted(payload.listingId(), payload.mediaId());
                }
                default -> log.debug("Ignoring eventType=[{}]", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process media-event: [{}]", message, ex);
            throw new RuntimeException(ex);
        }
    }
}