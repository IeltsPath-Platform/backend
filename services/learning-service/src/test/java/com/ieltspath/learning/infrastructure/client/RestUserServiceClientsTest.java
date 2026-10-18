package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The two reads of the User Service: the learning goal and the display names of a league table. Both forward the
 * caller's bearer token and, when anything goes wrong, answer with nothing so a report or a table still renders.
 */
class RestUserServiceClientsTest {
    private static final String BASE_URL = "http://user.test";
    private MockRestServiceServer server;
    private RestLearnerGoalClient goals;
    private RestDisplayNameClient names;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        goals = new RestLearnerGoalClient(restClient);
        names = new RestDisplayNameClient(restClient);
        signIn("learner-token");
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    private static void signIn(String token) {
        var jwt = Jwt.withTokenValue(token).header("alg", "HS256").subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var request = new MockHttpServletRequest();
        request.addHeader(SecurityHeaders.CORRELATION_ID, "corr-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void theGoalIsReadWithTheCallersTokenAndCorrelationId() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/" + user + "/learning-goal"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer learner-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "corr-1"))
                .andRespond(withSuccess("{\"targetBand\":7.0,\"examDate\":\"2027-03-01\"}", MediaType.APPLICATION_JSON));

        var goal = goals.goal(user).orElseThrow();

        assertEquals(0, new BigDecimal("7.0").compareTo(goal.targetBand()));
        assertEquals(LocalDate.parse("2027-03-01"), goal.examDate());
    }

    @Test
    void aGoalWithOnlyATargetBandHasNoExamDate() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/" + user + "/learning-goal"))
                .andRespond(withSuccess("{\"targetBand\":6.5}", MediaType.APPLICATION_JSON));

        var goal = goals.goal(user).orElseThrow();

        assertEquals(0, new BigDecimal("6.5").compareTo(goal.targetBand()));
        assertEquals(null, goal.examDate());
    }

    @Test
    void aLearnerWhoSetNoGoalHasNone() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/" + user + "/learning-goal"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertTrue(goals.goal(user).isEmpty());
    }

    @Test
    void aFailingUserServiceMeansNoGoal() {
        UUID user = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/" + user + "/learning-goal"))
                .andRespond(withServerError());
        assertTrue(goals.goal(user).isEmpty());
    }

    @Test
    void withoutABearerTokenNothingIsAskedOfTheUserService() {
        SecurityContextHolder.clearContext();

        assertTrue(goals.goal(UUID.randomUUID()).isEmpty());
        assertTrue(names.names(Set.of(UUID.randomUUID())).isEmpty());
        // server.verify() in cleanUp fails if any request had been sent.
    }

    @Test
    void displayNamesAreAskedInOneCallAndReturnedById() {
        UUID an = UUID.randomUUID();
        UUID binh = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/display-names")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer learner-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "corr-1"))
                .andExpect(jsonPath("$.ids.length()").value(2))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"%s\",\"displayName\":\"An\"},{\"id\":\"%s\",\"displayName\":\"Binh\"}]}"
                        .formatted(an, binh), MediaType.APPLICATION_JSON));

        var result = names.names(List.of(an, binh));

        assertEquals("An", result.get(an));
        assertEquals("Binh", result.get(binh));
    }

    @Test
    void anEntryWithoutANameIsLeftOut() {
        UUID an = UUID.randomUUID();
        UUID ghost = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/internal/users/display-names"))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"%s\",\"displayName\":\"An\"},{\"id\":\"%s\"}]}"
                        .formatted(an, ghost), MediaType.APPLICATION_JSON));

        var result = names.names(List.of(an, ghost));

        assertEquals(1, result.size());
        assertEquals("An", result.get(an));
    }

    @Test
    void noIdsMeansNoCall() {
        assertTrue(names.names(List.of()).isEmpty());
    }

    @Test
    void aFailingUserServiceMeansNoNames() {
        server.expect(requestTo(BASE_URL + "/internal/users/display-names")).andRespond(withServerError());
        assertTrue(names.names(List.of(UUID.randomUUID())).isEmpty());
    }
}
