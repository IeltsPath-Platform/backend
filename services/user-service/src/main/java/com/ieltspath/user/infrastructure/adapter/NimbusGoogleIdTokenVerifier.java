package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.application.port.GoogleIdTokenVerifier;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private final GoogleAuthProperties properties;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public NimbusGoogleIdTokenVerifier(GoogleAuthProperties properties) {
        this(properties, createRestOperations());
    }

    NimbusGoogleIdTokenVerifier(GoogleAuthProperties properties, RestOperations restOperations) {
        this.properties = properties;
        this.decoder = createDecoder(restOperations);
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        List<String> clientIds = configuredClientIds();
        if (clientIds.isEmpty()) {
            throw new GoogleSignInUnavailableException();
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            if (subject == null || subject.isBlank() || email == null || email.isBlank()) {
                throw new AuthenticationFailedException("Google ID token is invalid");
            }
            return new GoogleIdentity(subject, email, jwt.getClaimAsString("name"), jwt.getClaimAsString("hd"));
        } catch (BadJwtException invalidToken) {
            throw new AuthenticationFailedException("Google ID token is invalid");
        } catch (JwtException unavailable) {
            log.warn("Google ID token verification unavailable errorType={}", unavailable.getClass().getSimpleName());
            throw new GoogleSignInUnavailableException(unavailable);
        }
    }

    private NimbusJwtDecoder createDecoder(RestOperations restOperations) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI)
                .restOperations(restOperations)
                .cache(new GoogleJwksCache())
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();
        jwtDecoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), this::validateGoogleClaims));
        return jwtDecoder;
    }

    private OAuth2TokenValidatorResult validateGoogleClaims(Jwt jwt) {
        List<String> clientIds = configuredClientIds();
        String issuer = jwt.getClaimAsString("iss");
        boolean validIssuer = "accounts.google.com".equals(issuer) || "https://accounts.google.com".equals(issuer);
        boolean validAudience = jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(clientIds::contains);
        String authorizedParty = jwt.getClaimAsString("azp");
        boolean validAuthorizedParty = authorizedParty == null || authorizedParty.isBlank()
                || clientIds.contains(authorizedParty);
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        if (validIssuer && validAudience && validAuthorizedParty && emailVerified) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Google ID token claims", null));
    }

    private List<String> configuredClientIds() {
        return properties.getClientIds().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private static RestOperations createRestOperations() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(2));
        factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        return new RestTemplate(factory);
    }
}
