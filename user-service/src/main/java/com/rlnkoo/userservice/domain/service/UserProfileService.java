package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.api.me.dto.MeResponse;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.CurrentUser;
import com.rlnkoo.userservice.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MeResponse getMe() {

        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        UserEntity user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new UserNotFoundException(currentUser.userId()));

        return MeResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(Enum::name)
                                .collect(java.util.stream.Collectors.toSet())
                )
                .activated(user.isEnabled())
                .build();
    }
}