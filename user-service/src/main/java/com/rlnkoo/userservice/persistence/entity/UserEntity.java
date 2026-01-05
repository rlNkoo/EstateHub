package com.rlnkoo.userservice.persistence.entity;

import com.rlnkoo.userservice.domain.model.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

        @Id
        @GeneratedValue
        @Column(nullable = false,updatable = false)
        private UUID id;

        @Column(nullable = false, unique = true, length = 320)
        private String email;

        @Column(name = "password_hash", nullable = false)
        private String passwordHash;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(
                name = "user_roles",
                joinColumns = @JoinColumn(name = "user_id")
        )
        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false)
        @Builder.Default
        private Set<Role> roles = new HashSet<>();

        @Column(nullable = false)
        @Builder.Default
        private boolean enabled = false;

        @Column(name = "confirmed_at")
        private Instant confirmedAt;

        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        @Column(name = "first_name", length = 100)
        private String firstName;

        @Column(name = "last_name", length = 100)
        private String lastName;

        @Column(name = "phone_number", length = 30)
        private String phoneNumber;

        @PrePersist
        void prePersist() {
                this.createdAt = Instant.now();
                this.email = email.toLowerCase();
        }

        public void activate() {
                this.enabled = true;
                this.confirmedAt = Instant.now();
        }

        public void addRole(Role role) {
                this.roles.add(role);
        }

        public void changePassword(String newPasswordHash) {
                this.passwordHash = newPasswordHash;
        }

        public void updateProfile(String firstName, String lastName, String phoneNumber) {
                this.firstName = firstName;
                this.lastName = lastName;
                this.phoneNumber = phoneNumber;
        }
}