package com.rlnkoo.notificationservice.domain.service;

import com.rlnkoo.notificationservice.persistence.entity.UserEmailIndexEntity;
import com.rlnkoo.notificationservice.persistence.repository.UserEmailIndexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEmailIndexServiceTest {

    @Mock
    private UserEmailIndexRepository repository;

    @InjectMocks
    private UserEmailIndexService userEmailIndexService;

    @Test
    void shouldCreateUserEmailIndexWhenUserDoesNotExist() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        // when
        userEmailIndexService.upsert(userId, email);

        // then
        verify(repository).findByUserId(userId);

        ArgumentCaptor<UserEmailIndexEntity> entityCaptor = ArgumentCaptor.forClass(UserEmailIndexEntity.class);
        verify(repository).save(entityCaptor.capture());

        UserEmailIndexEntity savedEntity = entityCaptor.getValue();
        assertEquals(userId, savedEntity.getUserId());
        assertEquals(email, savedEntity.getEmail());
        assertNotNull(savedEntity.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingUserEmailWhenUserAlreadyExists() {
        // given
        UUID userId = UUID.randomUUID();
        String oldEmail = "old@example.com";
        String newEmail = "new@example.com";

        UserEmailIndexEntity existingEntity = UserEmailIndexEntity.of(userId, oldEmail);
        Instant previousUpdatedAt = existingEntity.getUpdatedAt();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existingEntity));

        // when
        userEmailIndexService.upsert(userId, newEmail);

        // then
        verify(repository).findByUserId(userId);
        verify(repository, never()).save(any(UserEmailIndexEntity.class));

        assertEquals(userId, existingEntity.getUserId());
        assertEquals(newEmail, existingEntity.getEmail());
        assertNotNull(existingEntity.getUpdatedAt());
        assertFalse(existingEntity.getUpdatedAt().isBefore(previousUpdatedAt));
    }

    @Test
    void shouldReturnEmailWhenUserEmailIndexExists() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        UserEmailIndexEntity entity = UserEmailIndexEntity.of(userId, email);

        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));

        // when
        String result = userEmailIndexService.requireEmail(userId);

        // then
        assertEquals(email, result);
        verify(repository).findByUserId(userId);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenUserEmailIndexDoesNotExist() {
        // given
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        // when + then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userEmailIndexService.requireEmail(userId)
        );

        assertEquals("Email not found for userId=" + userId, exception.getMessage());
        verify(repository).findByUserId(userId);
    }
}