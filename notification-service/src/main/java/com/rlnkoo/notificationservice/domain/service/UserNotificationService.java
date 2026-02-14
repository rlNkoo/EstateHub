package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.user.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationLogService logService;

    public void onUserRegistered(EventEnvelope<?> envelope, UserRegisteredV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("UserRegistered received. eventId={}, userId={}, email={}",
                envelope.eventId(), payload.userId(), payload.email());
    }

    public void onUserActivated(EventEnvelope<?> envelope, UserActivatedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("UserActivated received. eventId={}, userId={}, email={}, activatedAt={}",
                envelope.eventId(), payload.userId(), payload.email(), payload.activatedAt());
    }

    public void onPasswordResetRequested(EventEnvelope<?> envelope, PasswordResetRequestedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("PasswordResetRequested received. eventId={}, userId={}, email={}",
                envelope.eventId(), payload.userId(), payload.email());
    }

    public void onPasswordResetCompleted(EventEnvelope<?> envelope, PasswordResetCompletedV1Payload payload) {
        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        log.info("PasswordResetCompleted received. eventId={}, userId={}, email={}, completedAt={}",
                envelope.eventId(), payload.userId(), payload.email(), payload.completedAt());
    }

    private void logDuplicate(EventEnvelope<?> envelope) {
        log.info("Duplicate event ignored. eventId={}, eventType={}", envelope.eventId(), envelope.eventType());
    }
}