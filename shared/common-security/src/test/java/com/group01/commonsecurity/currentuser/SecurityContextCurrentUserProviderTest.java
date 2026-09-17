package com.group01.commonsecurity.currentuser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextCurrentUserProviderTest {
    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsCurrentUserFromJwtAuthentication() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer("urn:code-base:api-gateway")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", List.of("ADMIN"))
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                jwt.getSubject());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CurrentUser currentUser = provider.requireCurrentUser();

        assertThat(currentUser.id()).isEqualTo(userId);
        assertThat(currentUser.roles()).containsExactly("ADMIN");
        assertThat(currentUser.hasRole("ADMIN")).isTrue();
    }

    @Test
    void requireCurrentUserFailsWhenSecurityContextIsEmpty() {
        assertThat(provider.currentUser()).isEmpty();
        assertThatThrownBy(provider::requireCurrentUser)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
