package com.group01.apigateway.security;

import com.group01.apigateway.service.InternalJwtService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InternalJwtServiceTest {
    private static final String EXTERNAL_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Test
    void defaultInternalJwtPathsIncludeProtectedAuthMeRoute() {
        AuthProperties properties = authProperties(null);

        assertThat(properties.internalJwtPaths())
                .containsExactly("/api/users", "/auth/me");
    }

    @Test
    void createsMinimalGatewayInternalToken() throws Exception {
        AuthProperties properties = authProperties(List.of("/auth/me"));
        InternalJwtService service = new InternalJwtService(jwtEncoder(INTERNAL_SECRET), properties);
        UUID subject = UUID.randomUUID();
        Jwt externalToken = Jwt.withTokenValue("external")
                .header("alg", "HS256")
                .issuer("urn:code-base:auth")
                .expiresAt(Instant.now().plusSeconds(300))
                .subject(subject.toString())
                .claim("roles", List.of("CUSTOMER", "UNSUPPORTED"))
                .build();

        String token = service.createToken(externalToken);
        Map<String, Object> rawClaims = SignedJWT.parse(token).getJWTClaimsSet().getClaims();
        Jwt decoded = NimbusJwtDecoder.withSecretKey(secretKey(INTERNAL_SECRET))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token);

        assertThat(decoded.getClaimAsString("iss")).isEqualTo("urn:code-base:api-gateway");
        assertThat(decoded.getSubject()).isEqualTo(subject.toString());
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(decoded.hasClaim("aud")).isFalse();
        assertThat(decoded.getClaimAsString("type")).isNull();
        assertThat(decoded.getClaimAsString("email")).isNull();
        assertThat(decoded.getId()).isNull();
        assertThat(decoded.getHeaders()).doesNotContainKey("kid");
        assertThat(decoded.getExpiresAt()).isAfter(Instant.now());
        assertThat(rawClaims).containsOnlyKeys("iss", "sub", "exp", "roles");
    }

    private AuthProperties authProperties(List<String> internalJwtPaths) {
        return new AuthProperties(
                "urn:code-base:auth",
                EXTERNAL_SECRET,
                "urn:code-base:api-gateway",
                INTERNAL_SECRET,
                60,
                internalJwtPaths,
                "http://localhost:5173"
        );
    }

    private JwtEncoder jwtEncoder(String secret) {
        OctetSequenceKey key = new OctetSequenceKey.Builder(secretKey(secret)).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }

    private static SecretKeySpec secretKey(String secret) {
        return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    }
}
