package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.user.events.*;
import com.rlnkoo.notificationservice.mail.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationLogService logService;
    private final NotificationEmailService emailService;
    private final UserEmailIndexService userEmailIndexService;

    @Value("${notification.app.base-url}")
    private String baseUrl;

    public void onUserRegistered(EventEnvelope<?> envelope, UserRegisteredV1Payload payload) {
        userEmailIndexService.upsert(payload.userId(), payload.email());

        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) { logDuplicate(envelope); return; }

        try {
            var model = new HashMap<String, Object>();
            model.put("email", payload.email());
            model.put("activationToken", payload.activationToken());

            var msg = new EmailMessage(
                    payload.email(),
                    EmailSubjects.USER_REGISTERED,
                    EmailTemplates.USER_REGISTERED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send UserRegistered email. eventId=[{}], userId=[{}]",
                    envelope.eventId(), payload.userId(), ex);
            throw ex;
        }
    }

    public void onUserActivated(EventEnvelope<?> envelope, UserActivatedV1Payload payload) {
        userEmailIndexService.upsert(payload.userId(), payload.email());

        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        try {
            var model = new HashMap<String, Object>();
            model.put("email", payload.email());
            model.put("activatedAt", payload.activatedAt());

            var msg = new EmailMessage(
                    payload.email(),
                    EmailSubjects.USER_ACTIVATED,
                    EmailTemplates.USER_ACTIVATED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send UserActivated email. eventId=[{}], userId=[{}]",
                    envelope.eventId(), payload.userId(), ex);
            throw ex;
        }
    }

    public void onPasswordResetRequested(EventEnvelope<?> envelope, PasswordResetRequestedV1Payload payload) {
        userEmailIndexService.upsert(payload.userId(), payload.email());

        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) { logDuplicate(envelope); return; }

        try {
            var model = new HashMap<String, Object>();
            model.put("email", payload.email());
            model.put("resetToken", payload.resetToken());

            var msg = new EmailMessage(
                    payload.email(),
                    EmailSubjects.PASSWORD_RESET_REQUESTED,
                    EmailTemplates.PASSWORD_RESET_REQUESTED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send PasswordResetRequested email. eventId=[{}], userId=[{}]",
                    envelope.eventId(), payload.userId(), ex);
            throw ex;
        }
    }

    public void onPasswordResetCompleted(EventEnvelope<?> envelope, PasswordResetCompletedV1Payload payload) {
        userEmailIndexService.upsert(payload.userId(), payload.email());

        boolean firstTime = logService.tryMarkReceived(envelope, payload.email(), payload.userId(), null);
        if (!firstTime) {
            logDuplicate(envelope);
            return;
        }

        try {
            var model = new HashMap<String, Object>();
            model.put("email", payload.email());
            model.put("completedAt", payload.completedAt());

            var msg = new EmailMessage(
                    payload.email(),
                    EmailSubjects.PASSWORD_RESET_COMPLETED,
                    EmailTemplates.PASSWORD_RESET_COMPLETED,
                    model
            );

            emailService.send(msg);
            logService.markSent(envelope.eventId());

        } catch (Exception ex) {
            logService.markFailed(envelope.eventId(), ex);
            log.error("Failed to send PasswordResetCompleted email. eventId=[{}], userId=[{}]",
                    envelope.eventId(), payload.userId(), ex);
            throw ex;
        }
    }

    private void logDuplicate(EventEnvelope<?> envelope) {
        log.info("Duplicate event ignored. eventId=[{}], eventType=[{}]", envelope.eventId(), envelope.eventType());
    }
}