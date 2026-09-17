package com.group01.commonsecurity.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InternalJwtValidatorsTest {
    private static final String ISSUER = "urn:code-base:api-gateway";

    @Test
    void acceptsValidInternalJwtClaims() {
        assertThat(InternalJwtValidators.internalJwtValidator(ISSUER)
                .validate(jwt(ISSUER, UUID.randomUUID().toString(), List.of("ADMIN")))
                .hasErrors())
                .isFalse();
    }

    @Test
    void rejectsWrongIssuer() {
        assertThat(InternalJwtValidators.internalJwtValidator(ISSUER)
                .validate(jwt("urn:code-base:auth", UUID.randomUUID().toString(), List.of("ADMIN")))
                .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsUnsupportedRoles() {
        assertThat(InternalJwtValidators.internalJwtValidator(ISSUER)
                .validate(jwt(ISSUER, UUID.randomUUID().toString(), List.of("SUPER_ADMIN")))
                .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsNonUuidSubject() {
        assertThat(InternalJwtValidators.internalJwtValidator(ISSUER)
                .validate(jwt(ISSUER, "not-a-uuid", List.of("ADMIN")))
                .hasErrors())
                .isTrue();
    }

    private Jwt jwt(String issuer, String subject, List<String> roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", roles)
                .build();
    }
}
