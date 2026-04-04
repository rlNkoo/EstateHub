package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.api.me.dto.MeResponse;
import com.rlnkoo.userservice.api.me.dto.UpdateMeRequest;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.CurrentUser;
import com.rlnkoo.userservice.security.CurrentUserProvider;
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
class UserProfileServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void shouldReturnCurrentUserProfile() {
        // given
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(userId)
                .email("test@example.com")
                .roles(Set.of("USER"))
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER, Role.ADMIN))
                .firstName("Jan")
                .lastName("Kowalski")
                .phoneNumber("123456789")
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        MeResponse response = userProfileService.getMe();

        // then
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals(Set.of("USER", "ADMIN"), response.roles());
        assertTrue(response.activated());
        assertEquals("Jan", response.firstName());
        assertEquals("Kowalski", response.lastName());
        assertEquals("123456789", response.phoneNumber());

        verify(currentUserProvider).getCurrentUser();
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenCurrentUserNotFoundInGetMe() {
        // given
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(userId)
                .email("test@example.com")
                .roles(Set.of("USER"))
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when + then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userProfileService.getMe()
        );

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(currentUserProvider).getCurrentUser();
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldUpdateProfileAndReturnUpdatedResponse() {
        // given
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(userId)
                .email("test@example.com")
                .roles(Set.of("USER"))
                .build();

        UpdateMeRequest request = new UpdateMeRequest();
        request.setFirstName("Anna");
        request.setLastName("Nowak");
        request.setPhoneNumber("987654321");

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .firstName("Old")
                .lastName("Name")
                .phoneNumber("111111111")
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        MeResponse response = userProfileService.updateMe(request);

        // then
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals(Set.of("USER"), response.roles());
        assertTrue(response.activated());
        assertEquals("Anna", response.firstName());
        assertEquals("Nowak", response.lastName());
        assertEquals("987654321", response.phoneNumber());

        assertEquals("Anna", user.getFirstName());
        assertEquals("Nowak", user.getLastName());
        assertEquals("987654321", user.getPhoneNumber());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertSame(user, userCaptor.getValue());

        verify(currentUserProvider).getCurrentUser();
        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenCurrentUserNotFoundInUpdateMe() {
        // given
        UUID userId = UUID.randomUUID();

        CurrentUser currentUser = CurrentUser.builder()
                .userId(userId)
                .email("test@example.com")
                .roles(Set.of("USER"))
                .build();

        UpdateMeRequest request = new UpdateMeRequest();
        request.setFirstName("Anna");
        request.setLastName("Nowak");
        request.setPhoneNumber("987654321");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when + then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userProfileService.updateMe(request)
        );

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(currentUserProvider).getCurrentUser();
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }
}