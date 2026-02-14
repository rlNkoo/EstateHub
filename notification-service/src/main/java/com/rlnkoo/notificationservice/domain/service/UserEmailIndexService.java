package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.notificationservice.persistence.entity.UserEmailIndexEntity;
import com.rlnkoo.notificationservice.persistence.repository.UserEmailIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEmailIndexService {

    private final UserEmailIndexRepository repository;

    @Transactional
    public void upsert(UUID userId, String email) {
        repository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateEmail(email),
                        () -> repository.save(UserEmailIndexEntity.of(userId, email))
                );
    }

    @Transactional(readOnly = true)
    public String requireEmail(UUID userId) {
        return repository.findByUserId(userId)
                .map(UserEmailIndexEntity::getEmail)
                .orElseThrow(() -> new IllegalStateException("Email not found for userId=" + userId));
    }
}