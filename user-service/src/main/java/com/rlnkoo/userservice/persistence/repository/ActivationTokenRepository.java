package com.rlnkoo.userservice.persistence.repository;

import com.rlnkoo.userservice.domain.model.ActivationToken;
import com.rlnkoo.userservice.persistence.entity.ActivationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, UUID> {

    Optional<ActivationTokenEntity> findByTokenHashAndUsedAtIsNull(String tokenHash);

    long deleteByExpiresAtBefore(Instant cutoff);
}