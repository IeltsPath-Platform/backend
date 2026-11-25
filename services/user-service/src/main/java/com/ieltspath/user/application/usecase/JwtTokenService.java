package com.ieltspath.user.application.usecase;

import com.ieltspath.user.config.AuthTokenProperties;
import com.ieltspath.user.domain.aggregate.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final AuthTokenProperties properties;

    public String createAccessToken(User user, UUID sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = Instant.now().plusSeconds(properties.accessTokenMaxAgeSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.externalJwtIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionId.toString())
                .claim("roles", user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .sorted()
                        .toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
