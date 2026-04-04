package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.domain.exception.InvalidRoleException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        // given
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("USER");

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when + then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> adminUserService.changeRoles(userId, roles)
        );

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowInvalidRoleExceptionWhenRoleIsInvalid() {
        // given
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("USER", "SUPERADMIN");

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when + then
        InvalidRoleException exception = assertThrows(
                InvalidRoleException.class,
                () -> adminUserService.changeRoles(userId, roles)
        );

        assertEquals("Invalid role: SUPERADMIN", exception.getMessage());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldChangeRolesAndReturnNewRoles() {
        // given
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("USER", "ADMIN");

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        Set<Role> result = adminUserService.changeRoles(userId, roles);

        // then
        assertEquals(Set.of(Role.USER, Role.ADMIN), result);
        assertEquals(Set.of(Role.USER, Role.ADMIN), user.getRoles());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertSame(user, userCaptor.getValue());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldTrimAndUppercaseRolesBeforeSaving() {
        // given
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of(" admin ", " user ");

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        Set<Role> result = adminUserService.changeRoles(userId, roles);

        // then
        assertEquals(Set.of(Role.USER, Role.ADMIN), result);
        assertEquals(Set.of(Role.USER, Role.ADMIN), user.getRoles());

        verify(userRepository).save(user);
        verify(userRepository).findById(userId);
    }
}