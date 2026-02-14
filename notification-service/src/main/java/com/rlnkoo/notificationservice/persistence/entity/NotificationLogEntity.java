package com.rlnkoo.notificationservice.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "notification_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_log_event_id", columnNames = "event_id")
        },
        indexes = {
                @Index(name = "idx_notification_log_status", columnList = "status"),
                @Index(name = "idx_notification_log_created_at", columnList = "created_at")
        }
)
public class NotificationLogEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static NotificationLogEntity received(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String recipientEmail,
            UUID userId,
            UUID listingId
    ) {
        NotificationLogEntity e = new NotificationLogEntity();
        e.id = UUID.randomUUID();
        e.eventId = eventId;
        e.eventType = eventType;
        e.occurredAt = occurredAt;
        e.status = NotificationStatus.RECEIVED;
        e.recipientEmail = recipientEmail;
        e.userId = userId;
        e.listingId = listingId;
        e.attempts = 0;
        e.createdAt = Instant.now();
        e.updatedAt = e.createdAt;
        return e;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.updatedAt = Instant.now();
        this.attempts = this.attempts + 1;
        this.lastError = truncate(error, 2000);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}