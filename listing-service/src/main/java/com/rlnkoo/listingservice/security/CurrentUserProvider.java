package com.rlnkoo.listingservice.security;

import com.rlnkoo.commonsecurity.Claims;
import com.rlnkoo.commonsecurity.Roles;
import com.rlnkoo.listingservice.domain.exception.AuthenticationRequiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class CurrentUserProvider {

    public Optional<CurrentUser> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return Optional.empty();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString(Claims.EMAIL);

        List<String> rolesClaim = jwt.getClaimAsStringList(Claims.ROLES);
        Set<String> roles = rolesClaim == null ? Set.of() : Set.copyOf(rolesClaim);

        return Optional.of(CurrentUser.builder()
                .userId(userId)
                .email(email)
                .roles(roles)
                .build());
    }

    public CurrentUser requireCurrentUser() {
        return getCurrentUserOptional().orElseThrow(AuthenticationRequiredException::new);
    }

    public boolean hasRole(CurrentUser user, String role) {
        return user.roles().contains(role) || user.roles().contains(Roles.asAuthority(role));
    }
}