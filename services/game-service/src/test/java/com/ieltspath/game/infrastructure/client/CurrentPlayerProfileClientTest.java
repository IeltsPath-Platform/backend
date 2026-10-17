package com.ieltspath.game.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CurrentPlayerProfileClientTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void fetchesVerifiedProfileAndRejectsMismatchedIdentity() {
        var id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            Jwt.withTokenValue("test-token").header("alg", "HS256").subject(id.toString()).build(), java.util.List.of()));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new CurrentPlayerProfileClient(builder, "http://user.test");
        org.springframework.test.util.ReflectionTestUtils.setField(client, "client", builder.baseUrl("http://user.test").build());
        server.expect(requestTo("http://user.test/api/users/me"))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andRespond(withSuccess("{\"id\":\"" + id + "\",\"fullName\":\"Learner Name\"}", MediaType.APPLICATION_JSON));
        assertEquals("Learner Name", client.displayName());
        server.verify();
        server.reset();
        server.expect(requestTo("http://user.test/api/users/me"))
            .andRespond(withSuccess("{\"id\":\"" + UUID.randomUUID() + "\",\"fullName\":\"Other\"}", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class, client::displayName);
        server.verify();
    }

    @Test void requiresAuthenticatedUser() {
        var client = new CurrentPlayerProfileClient(RestClient.builder(), "http://user.test");
        assertThrows(IllegalStateException.class, client::displayName);
    }
}
