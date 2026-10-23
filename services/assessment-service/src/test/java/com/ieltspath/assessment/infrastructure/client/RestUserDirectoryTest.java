package com.ieltspath.assessment.infrastructure.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestUserDirectoryTest {
    private static final String BASE_URL = "http://user.test";

    private MockRestServiceServer server;
    private RestUserDirectory directory;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        directory = new RestUserDirectory(builder.build());
        Jwt jwt = Jwt.withTokenValue("gateway-token").header("alg", "none").subject("author")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void namesComeBackByIdWithTheCallersBearer() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/display-names")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer gateway-token"))
                .andExpect(header("X-Correlation-Id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"" + a + "\",\"displayName\":\"Linh\"},"
                        + "{\"id\":\"" + b + "\",\"displayName\":null}]}", MediaType.APPLICATION_JSON));

        Map<UUID, String> names = directory.displayNames(List.of(a, b));

        assertEquals(Map.of(a, "Linh"), names);
        server.verify();
    }

    @Test
    void moreThanFiftyIdsAreSentInBatches() {
        List<UUID> ids = IntStream.range(0, 120).mapToObj(i -> UUID.randomUUID()).toList();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3),
                        requestTo(BASE_URL + "/internal/users/display-names"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertTrue(directory.displayNames(ids).isEmpty());
        server.verify();
    }

    @Test
    void duplicateIdsAreSentOnce() {
        UUID a = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/display-names"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.ids.length()")
                        .value(1))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        directory.displayNames(List.of(a, a, a));

        server.verify();
    }

    @Test
    void aFailureMeansNoNamesAtAll() {
        UUID a = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/display-names")).andRespond(withServerError());

        assertTrue(directory.displayNames(List.of(a)).isEmpty());
    }

    @Test
    void withoutAGatewayTokenNothingIsCalled() {
        SecurityContextHolder.clearContext();

        assertTrue(directory.displayNames(List.of(UUID.randomUUID())).isEmpty());
        server.verify();
    }

    @Test
    void anEmptyListCallsNothing() {
        assertTrue(directory.displayNames(List.of()).isEmpty());
        server.verify();
    }
}
