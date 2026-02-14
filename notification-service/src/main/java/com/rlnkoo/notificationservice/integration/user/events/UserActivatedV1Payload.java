package com.rlnkoo.notificationservice.integration.user.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserActivatedV1Payload(
        UUID userId,
        String email,
        Instant activatedAt
) {}