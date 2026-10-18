package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.commonsecurity.config.InternalJwtProperties;
import com.ieltspath.commonsecurity.jwt.HmacKeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

/** Signs a short-lived least-privilege internal token for assessment's scheduled worker calls. */
@Component
public class AssessmentServiceTokenProvider {
    private final InternalJwtProperties properties;
    private final JwtEncoder encoder;
    private final Clock clock;

    @Autowired
    public AssessmentServiceTokenProvider(InternalJwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AssessmentServiceTokenProvider(InternalJwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SecretKey secret = HmacKeyFactory.secretKey(properties.internalJwtSecret(), "app.auth.internal-jwt-secret");
        OctetSequenceKey key = new OctetSequenceKey.Builder(secret).algorithm(com.nimbusds.jose.JWSAlgorithm.HS256).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    public String authorizationHeader() {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.internalJwtIssuer())
                .subject(UUID.nameUUIDFromBytes("assessment-service".getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString())
                .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(5))).claim("roles", List.of("CUSTOMER")).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
