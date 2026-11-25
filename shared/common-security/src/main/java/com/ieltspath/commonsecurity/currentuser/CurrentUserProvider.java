package com.ieltspath.commonsecurity.currentuser;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.ieltspath.commonsecurity.jwt.InternalJwtClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CurrentUserProvider {
    Optional<CurrentUser> currentUser();

    default Optional<UUID> currentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return Optional.empty();
        String sessionId = jwt.getClaimAsString(InternalJwtClaims.SESSION_ID);
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        try { return Optional.of(UUID.fromString(sessionId)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    CurrentUser requireCurrentUser();

    default UUID requireUserId() {
        return requireCurrentUser().id();
    }

    default Set<String> requireRoles() {
        return requireCurrentUser().roles();
    }
}
