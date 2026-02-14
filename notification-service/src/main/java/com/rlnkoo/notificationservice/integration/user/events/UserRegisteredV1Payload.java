package com.rlnkoo.notificationservice.integration.user.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRegisteredV1Payload(
        UUID userId,
        String email,
        String activationToken
) {}