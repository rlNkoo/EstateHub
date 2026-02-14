package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.notificationservice.integration.user.events.*;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationService {

    public void onUserRegistered(EventEnvelope<?> envelope, UserRegisteredV1Payload payload) {
    }

    public void onUserActivated(EventEnvelope<?> envelope, UserActivatedV1Payload payload) {
    }

    public void onPasswordResetRequested(EventEnvelope<?> envelope, PasswordResetRequestedV1Payload payload) {
    }

    public void onPasswordResetCompleted(EventEnvelope<?> envelope, PasswordResetCompletedV1Payload payload) {
    }
}