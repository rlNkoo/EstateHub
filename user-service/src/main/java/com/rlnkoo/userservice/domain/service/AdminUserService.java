package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.domain.exception.InvalidRoleException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional
    public Set<Role> changeRoles(UUID userId, Set<String> roles) {
        log.info("Change roles request targetUserId=[{}] roles=[{}]", userId, roles);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Change roles failed: user not found targetUserId=[{}]", userId);
                    return new UserNotFoundException(userId);
                });

        Set<Role> newRoles = roles.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(r -> {
                    try {
                        return Role.valueOf(r);
                    } catch (Exception e) {
                        log.warn("Change roles failed: invalid role=[{}] targetUserId=[{}]", r, userId);
                        throw new InvalidRoleException(r);
                    }
                })
                .collect(Collectors.toSet());

        user.setRoles(newRoles);
        userRepository.save(user);
        log.info("Roles changed targetUserId=[{}] newRoles=[{}]", userId, newRoles);

        return newRoles;
    }
}