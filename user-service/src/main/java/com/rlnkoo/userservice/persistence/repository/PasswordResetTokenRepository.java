package com.rlnkoo.userservice.persistence.repository;

import com.rlnkoo.userservice.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNull(String tokenHash);
}