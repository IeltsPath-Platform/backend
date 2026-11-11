package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.exception.GoogleSignInUnavailableException;
import com.ieltspath.user.application.result.GoogleIdentity;
import com.ieltspath.user.config.GoogleAuthProperties;
import com.ieltspath.user.domain.exception.AuthenticationFailedException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NimbusGoogleIdTokenVerifierTest {
    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private RSAKey signingKey;
    private RSAKey rotatedKey;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private NimbusGoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("google-key").generate();
        rotatedKey = new RSAKeyGenerator(2048).keyID("rotated-key").generate();
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        GoogleAuthProperties properties = new GoogleAuthProperties();
        properties.setClientIds(List.of("web-client-id"));
        verifier = new NimbusGoogleIdTokenVerifier(properties, restTemplate);
    }

    @Test
    void verifiesSignedGoogleClaimsAndCachesJwks() throws Exception {
        expectJwks(signingKey);
        GoogleIdentity identity = verifier.verify(token(signingKey, "https://accounts.google.com",
                List.of("web-client-id"), "web-client-id", true, Instant.now().plusSeconds(300)));
        GoogleIdentity cached = verifier.verify(token(signingKey, "accounts.google.com",
                List.of("web-client-id"), null, true, Instant.now().plusSeconds(300)));

        assertThat(identity).isEqualTo(new GoogleIdentity("subject", "person@school.edu", "Person", "school.edu"));
        assertThat(cached.subject()).isEqualTo("subject");
        server.verify();
    }

    @Test
    void rejectsWrongAudienceAuthorizedPartyIssuerExpiryAndUnverifiedEmail() throws Exception {
        expectJwks(signingKey);
        String validToken = token(signingKey, "https://accounts.google.com", List.of("web-client-id"),
                null, true, Instant.now().plusSeconds(300));
        verifier.verify(validToken);

        assertInvalid(token(signingKey, "https://accounts.google.com", List.of("other-client-id"),
                null, true, Instant.now().plusSeconds(300)));
        assertInvalid(token(signingKey, "https://accounts.google.com", List.of("web-client-id"),
                "other-client-id", true, Instant.now().plusSeconds(300)));
        assertInvalid(token(signingKey, "https://evil.example", List.of("web-client-id"),
                null, true, Instant.now().plusSeconds(300)));
        assertInvalid(token(signingKey, "https://accounts.google.com", List.of("web-client-id"),
                null, false, Instant.now().plusSeconds(300)));
        assertInvalid(token(signingKey, "https://accounts.google.com", List.of("web-client-id"),
                null, true, Instant.now().minusSeconds(1)));
        server.verify();
    }

    @Test
    void refreshesJwksWhenGoogleRotatesTheSigningKey() throws Exception {
        server.expect(requestTo(JWKS_URI)).andRespond(withSuccess(new JWKSet(signingKey.toPublicJWK()).toString(),
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(JWKS_URI)).andRespond(withSuccess(new JWKSet(rotatedKey.toPublicJWK()).toString(),
                MediaType.APPLICATION_JSON));
        verifier.verify(token(signingKey, "https://accounts.google.com", List.of("web-client-id"),
                null, true, Instant.now().plusSeconds(300)));

        assertThat(verifier.verify(token(rotatedKey, "https://accounts.google.com", List.of("web-client-id"),
                null, true, Instant.now().plusSeconds(300))).subject()).isEqualTo("subject");
        server.verify();
    }

    @Test
    void reportsJwksFailureAsUnavailableAndEmptyClientConfigurationAsUnavailable() throws Exception {
        server.expect(requestTo(JWKS_URI)).andRespond(withServerError());
        assertThatThrownBy(() -> verifier.verify(token(signingKey, "https://accounts.google.com",
                List.of("web-client-id"), null, true, Instant.now().plusSeconds(300))))
                .isInstanceOf(GoogleSignInUnavailableException.class);
        server.verify();

        GoogleAuthProperties empty = new GoogleAuthProperties();
        NimbusGoogleIdTokenVerifier unconfigured = new NimbusGoogleIdTokenVerifier(empty, new RestTemplate());
        assertThatThrownBy(() -> unconfigured.verify("unused"))
                .isInstanceOf(GoogleSignInUnavailableException.class);
    }

    private void expectJwks(RSAKey key) {
        server.expect(requestTo(JWKS_URI)).andRespond(withSuccess(new JWKSet(key.toPublicJWK()).toString(),
                MediaType.APPLICATION_JSON));
    }

    private String token(RSAKey key, String issuer, List<String> audience, String authorizedParty,
                         boolean emailVerified, Instant expiresAt) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("subject")
                .audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(1)))
                .expirationTime(Date.from(expiresAt))
                .claim("email", "person@school.edu")
                .claim("email_verified", emailVerified)
                .claim("name", "Person")
                .claim("hd", "school.edu");
        if (authorizedParty != null) claims.claim("azp", authorizedParty);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(AuthenticationFailedException.class);
    }
}
