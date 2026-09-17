package com.group01.commonsecurity.jwt;

import com.group01.commonsecurity.role.CanonicalRoles;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.Set;
import java.util.UUID;

public final class InternalJwtValidators {
    private InternalJwtValidators() {
    }

    public static OAuth2TokenValidator<Jwt> internalJwtValidator(String issuer) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                subjectValidator(),
                canonicalRolesValidator()
        );
    }

    public static OAuth2TokenValidator<Jwt> subjectValidator() {
        return jwt -> {
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                return invalidSubject();
            }
            try {
                UUID.fromString(subject);
                return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException exception) {
                return invalidSubject();
            }
        };
    }

    public static OAuth2TokenValidator<Jwt> canonicalRolesValidator() {
        return jwt -> {
            Set<String> roles = InternalJwtAuthorities.claimRoles(jwt);
            return !roles.isEmpty() && CanonicalRoles.ALL.containsAll(roles)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "The token contains an unsupported role", null));
        };
    }

    private static OAuth2TokenValidatorResult invalidSubject() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "The subject must be a UUID", null));
    }
}
