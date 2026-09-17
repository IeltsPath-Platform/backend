package com.group01.commonsecurity.currentuser;

import com.group01.commonsecurity.jwt.InternalJwtAuthorities;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(fromJwt(jwt));
        }
        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CurrentUser(UUID.fromString(name), roles(authentication)));
    }

    @Override
    public CurrentUser requireCurrentUser() {
        return currentUser().orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException("Current user is not available"));
    }

    private CurrentUser fromJwt(Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()), InternalJwtAuthorities.claimRoles(jwt));
    }

    private Set<String> roles(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_") && value.length() > "ROLE_".length()) {
                roles.add(value.substring("ROLE_".length()));
            }
        }
        return roles;
    }
}
