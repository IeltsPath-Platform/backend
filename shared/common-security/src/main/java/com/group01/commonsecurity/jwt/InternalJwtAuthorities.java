package com.group01.commonsecurity.jwt;

import com.group01.commonsecurity.role.CanonicalRoles;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InternalJwtAuthorities {

    private InternalJwtAuthorities() {
    }

    public static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return claimRoles(jwt).stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static Set<String> claimRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        Object value = jwt.getClaim(InternalJwtClaims.ROLES);
        if (value instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
        } else if (value instanceof String role && !role.isBlank()) {
            roles.add(role);
        }
        return roles;
    }

    /**
     * Tap hop cac vai tro hop le cua he thong.
     * @deprecated Su dung {@link CanonicalRoles#ALL} thay the.
     */
    @Deprecated(forRemoval = true)
    public static final Set<String> CANONICAL_ROLES = CanonicalRoles.ALL;
}
