package com.rlnkoo.userservice.api.admin;

import com.rlnkoo.userservice.api.admin.dto.ChangeRolesRequest;
import com.rlnkoo.userservice.api.admin.dto.ChangeRolesResponse;
import com.rlnkoo.userservice.domain.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ChangeRolesResponse changeRoles(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody ChangeRolesRequest request
    ) {
        var newRoles = adminUserService.changeRoles(userId, request.getRoles());

        Set<String> roleNames = newRoles.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return ChangeRolesResponse.builder()
                .userId(userId)
                .roles(roleNames)
                .message("Roles updated successfully")
                .build();
    }
}