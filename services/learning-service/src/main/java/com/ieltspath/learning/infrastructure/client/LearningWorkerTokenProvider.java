package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.config.InternalJwtProperties;
import com.ieltspath.commonsecurity.jwt.HmacKeyFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Signs a short-lived, least-privilege internal token for the calls Learning makes when no learner's request is running: a
 * message consumer applying an event reads Content (rubrics, error types, the topic sequence), which only a gateway-signed
 * token may do. The token names Learning itself and the learner role, the same as Assessment's scheduled worker; it carries
 * no learner identity, and only reads that need none use it.
 */
@Component
public class LearningWorkerTokenProvider {
    private final InternalJwtProperties properties;
    private final JwtEncoder encoder;
    private final Clock clock;

    @Autowired
    public LearningWorkerTokenProvider(InternalJwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LearningWorkerTokenProvider(InternalJwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SecretKey secret = HmacKeyFactory.secretKey(properties.internalJwtSecret(), "app.auth.internal-jwt-secret");
        OctetSequenceKey key = new OctetSequenceKey.Builder(secret).algorithm(com.nimbusds.jose.JWSAlgorithm.HS256).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    public String token() {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.internalJwtIssuer())
                .subject(UUID.nameUUIDFromBytes("learning-service".getBytes(StandardCharsets.UTF_8)).toString())
                .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(5))).claim("roles", List.of("CUSTOMER")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
