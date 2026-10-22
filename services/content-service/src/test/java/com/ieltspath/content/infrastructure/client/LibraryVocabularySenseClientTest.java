package com.ieltspath.content.infrastructure.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;

class LibraryVocabularySenseClientTest {
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void requestsSenseBatchWithBearerAndReturnsIds() {
        UUID senseId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://library.test").build();
        LibraryVocabularySenseClient client = new LibraryVocabularySenseClient(restClient);
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "author").build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        server.expect(requestTo("http://library.test/api/content/vocabulary/senses?ids=" + senseId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("[{\"senseId\":\"" + senseId + "\"}]", MediaType.APPLICATION_JSON));

        assertThat(client.findActiveSenseIds(List.of(senseId))).containsExactly(senseId);
        server.verify();
    }
}
