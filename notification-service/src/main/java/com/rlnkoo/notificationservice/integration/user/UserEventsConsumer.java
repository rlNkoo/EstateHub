package com.rlnkoo.notificationservice.integration.user;

import com.rlnkoo.commonevents.Topics;
import com.rlnkoo.notificationservice.domain.service.UserNotificationService;
import com.rlnkoo.notificationservice.integration.kafka.EventEnvelopeReader;
import com.rlnkoo.notificationservice.integration.user.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventsConsumer {

    private final EventEnvelopeReader reader;
    private final UserNotificationService userNotificationService;

    @KafkaListener(topics = Topics.USER_EVENTS, groupId = "notification-service-user")
    public void onMessage(String message) {
        try {
            var envelope = reader.read(message);

            if (envelope.eventId() == null || envelope.eventType() == null || envelope.payload() == null) {
                log.warn("Invalid user envelope: missing eventId/eventType/payload. raw={}", message);
                return;
            }

            switch (envelope.eventType()) {
                case "UserRegisteredV1" -> {
                    var payload = reader.readPayload(envelope.payload(), UserRegisteredV1Payload.class);
                    if (payload.userId() == null || payload.email() == null || payload.activationToken() == null) {
                        log.warn("UserRegisteredV1 missing required fields. eventId={}, payload={}",
                                envelope.eventId(), payload);
                        return;
                    }
                    userNotificationService.onUserRegistered(envelope, payload);
                }
                case "UserActivatedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), UserActivatedV1Payload.class);
                    if (payload.userId() == null || payload.email() == null || payload.activatedAt() == null) {
                        log.warn("UserActivatedV1 missing required fields. eventId={}, payload={}",
                                envelope.eventId(), payload);
                        return;
                    }
                    userNotificationService.onUserActivated(envelope, payload);
                }
                case "PasswordResetRequestedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), PasswordResetRequestedV1Payload.class);
                    if (payload.userId() == null || payload.email() == null || payload.resetToken() == null) {
                        log.warn("PasswordResetRequestedV1 missing required fields. eventId={}, payload={}",
                                envelope.eventId(), payload);
                        return;
                    }
                    userNotificationService.onPasswordResetRequested(envelope, payload);
                }
                case "PasswordResetCompletedV1" -> {
                    var payload = reader.readPayload(envelope.payload(), PasswordResetCompletedV1Payload.class);
                    if (payload.userId() == null || payload.email() == null || payload.completedAt() == null) {
                        log.warn("PasswordResetCompletedV1 missing required fields. eventId={}, payload={}",
                                envelope.eventId(), payload);
                        return;
                    }
                    userNotificationService.onPasswordResetCompleted(envelope, payload);
                }
                default -> log.debug("Ignoring unknown user eventType={}", envelope.eventType());
            }

        } catch (Exception ex) {
            log.error("Cannot process user event message. raw={}", message, ex);
            throw ex; // fail fast jak w listing-service consumerach
        }
    }
}