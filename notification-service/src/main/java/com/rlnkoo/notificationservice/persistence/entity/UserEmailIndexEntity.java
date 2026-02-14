package com.rlnkoo.notificationservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_email_index")
public class UserEmailIndexEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserEmailIndexEntity of(UUID userId, String email) {
        UserEmailIndexEntity e = new UserEmailIndexEntity();
        e.userId = userId;
        e.email = email;
        e.updatedAt = Instant.now();
        return e;
    }

    public void updateEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
    }
}