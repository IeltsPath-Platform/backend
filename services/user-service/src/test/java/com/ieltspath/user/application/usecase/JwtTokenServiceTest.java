package com.ieltspath.user.application.usecase;

import com.ieltspath.user.config.AuthTokenProperties;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.vo.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {
    @Test
    void addsUniqueTokenIdIssueTimeAndSessionIdWithoutChangingExistingClaims() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtClaimsSet claims = invocation.<JwtEncoderParameters>getArgument(0).getClaims();
            return Jwt.withTokenValue("signed")
                    .header("alg", "HS256")
                    .issuedAt(claims.getIssuedAt())
                    .expiresAt(claims.getExpiresAt())
                    .subject(claims.getSubject())
                    .claims(values -> values.putAll(claims.getClaims()))
                    .build();
        });
        var service = new JwtTokenService(encoder,
                new AuthTokenProperties("issuer", "secret", 3600, 604800, 10, 0));
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = User.builder().id(userId).roles(Set.of(Role.builder().name(RoleName.CUSTOMER).build())).build();

        service.createAccessToken(user, sessionId);
        service.createAccessToken(user, sessionId);

        org.mockito.ArgumentCaptor<JwtEncoderParameters> captor =
                org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        org.mockito.Mockito.verify(encoder, org.mockito.Mockito.times(2)).encode(captor.capture());
        var claims = captor.getAllValues().get(0).getClaims();
        var secondClaims = captor.getAllValues().get(1).getClaims();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getId()).isNotBlank().isNotEqualTo(secondClaims.getId());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.<String>getClaim("sid")).isEqualTo(sessionId.toString());
        assertThat(claims.getIssuer().toString()).isEqualTo("issuer");
        assertThat(claims.<java.util.List<String>>getClaim("roles")).isEqualTo(java.util.List.of("CUSTOMER"));
        assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
    }
}
