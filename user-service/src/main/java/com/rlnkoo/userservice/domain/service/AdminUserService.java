package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.domain.exception.InvalidRoleException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional
    public Set<Role> changeRoles(UUID userId, Set<String> roles) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Set<Role> newRoles = roles.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(r -> {
                    try {
                        return Role.valueOf(r);
                    } catch (Exception e) {
                        throw new InvalidRoleException(r);
                    }
                })
                .collect(Collectors.toSet());

        user.setRoles(newRoles);
        userRepository.save(user);

        return newRoles;
    }
}