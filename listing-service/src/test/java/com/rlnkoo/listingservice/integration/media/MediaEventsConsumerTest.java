package com.rlnkoo.listingservice.integration.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rlnkoo.listingservice.domain.service.ListingMediaSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaEventsConsumerTest {

    @Mock
    private ListingMediaSyncService syncService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MediaEventsConsumer mediaEventsConsumer;

    @Test
    void shouldHandlePhotoUploadedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "listingId": "%s",
                    "mediaId": "%s"
                  }
                }
                """.formatted(listingId, mediaId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verify(syncService).onPhotoUploaded(listingId, mediaId);
        verify(syncService, never()).onPhotoDeleted(any(), any());
        verifyNoMoreInteractions(syncService);
    }

    @Test
    void shouldHandlePhotoDeletedEvent() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoDeletedV1",
                  "payload": {
                    "listingId": "%s",
                    "mediaId": "%s"
                  }
                }
                """.formatted(listingId, mediaId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verify(syncService).onPhotoDeleted(listingId, mediaId);
        verify(syncService, never()).onPhotoUploaded(any(), any());
        verifyNoMoreInteractions(syncService);
    }

    @Test
    void shouldIgnoreUnknownEventType() {
        // given
        String message = """
                {
                  "eventType": "PhotoReorderedV1",
                  "payload": {
                    "listingId": "%s",
                    "mediaId": "%s"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnoreMessageWithoutEventType() {
        // given
        String message = """
                {
                  "payload": {
                    "listingId": "%s",
                    "mediaId": "%s"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnorePhotoUploadedEventWhenListingIdIsMissing() {
        // given
        UUID mediaId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "mediaId": "%s"
                  }
                }
                """.formatted(mediaId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnorePhotoUploadedEventWhenMediaIdIsMissing() {
        // given
        UUID listingId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "listingId": "%s"
                  }
                }
                """.formatted(listingId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnorePhotoDeletedEventWhenListingIdIsMissing() {
        // given
        UUID mediaId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoDeletedV1",
                  "payload": {
                    "mediaId": "%s"
                  }
                }
                """.formatted(mediaId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnorePhotoDeletedEventWhenMediaIdIsMissing() {
        // given
        UUID listingId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoDeletedV1",
                  "payload": {
                    "listingId": "%s"
                  }
                }
                """.formatted(listingId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verifyNoInteractions(syncService);
    }

    @Test
    void shouldIgnorePhotoUploadedEventWithAdditionalUnknownFields() {
        // given
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        String message = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "listingId": "%s",
                    "mediaId": "%s",
                    "extraField": "ignored"
                  },
                  "metadata": {
                    "source": "media-service"
                  }
                }
                """.formatted(listingId, mediaId);

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when
        mediaEventsConsumer.onMessage(message);

        // then
        verify(syncService).onPhotoUploaded(listingId, mediaId);
        verifyNoMoreInteractions(syncService);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenJsonIsInvalid() {
        // given
        String invalidMessage = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "listingId": "not-closed-json"
                """;

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when + then
        assertThrows(RuntimeException.class, () -> mediaEventsConsumer.onMessage(invalidMessage));

        verifyNoInteractions(syncService);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenPayloadContainsInvalidUuid() {
        // given
        String message = """
                {
                  "eventType": "PhotoUploadedV1",
                  "payload": {
                    "listingId": "not-a-uuid",
                    "mediaId": "%s"
                  }
                }
                """.formatted(UUID.randomUUID());

        mediaEventsConsumer = new MediaEventsConsumer(objectMapper, syncService);

        // when + then
        assertThrows(RuntimeException.class, () -> mediaEventsConsumer.onMessage(message));

        verifyNoInteractions(syncService);
    }
}