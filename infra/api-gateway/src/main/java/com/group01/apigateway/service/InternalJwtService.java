package com.group01.apigateway.service;

import com.group01.apigateway.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InternalJwtService {
    private static final Set<String> CANONICAL_ROLES = Set.of("ADMIN", "LEARNER");

    private final JwtEncoder internalJwtEncoder;
    private final AuthProperties authProperties;

    public String createToken(Jwt externalToken) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.internalJwtIssuer())
                .expiresAt(Instant.now().plusSeconds(authProperties.internalTokenMaxAgeSeconds()))
                .subject(externalToken.getSubject())
                .claim("roles", roles(externalToken))
                .claims(values -> values.remove("iat"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();                                                                                                                                                                                      return internalJwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private List<String> roles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        Object value = jwt.getClaim("roles");
        if (value instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
        } else if (value instanceof String role && !role.isBlank()) {
            roles.add(role);
        }
        roles.retainAll(CANONICAL_ROLES);
        return roles.stream().sorted().toList();
    }
}
