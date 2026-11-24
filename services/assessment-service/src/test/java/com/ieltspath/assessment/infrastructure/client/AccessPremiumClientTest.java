package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.commonresilience.ResilientCall;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class AccessPremiumClientTest {
    private static final String BASE_URL = "http://access.test";

    @Test
    void callsTheInternalPremiumRouteWithTheWorkerToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AssessmentServiceTokenProvider tokens = mock(AssessmentServiceTokenProvider.class);
        when(tokens.authorizationHeader()).thenReturn("Bearer assessment-worker");
        AccessPremiumClient client = new AccessPremiumClient(builder.build(), ResilientCall.isolatedDefault(), tokens);
        UUID userId = UUID.randomUUID();

        server.expect(requestTo(BASE_URL + "/internal/access/users/" + userId + "/premium"))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer assessment-worker"))
                .andRespond(withSuccess("{\"premium\":true}", MediaType.APPLICATION_JSON));

        assertTrue(client.isPremium(userId));
        server.verify();
    }

    @Test
    void accessFailureFailsClosed() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AssessmentServiceTokenProvider tokens = mock(AssessmentServiceTokenProvider.class);
        when(tokens.authorizationHeader()).thenReturn("Bearer assessment-worker");
        AccessPremiumClient client = new AccessPremiumClient(builder.build(), ResilientCall.isolatedDefault(), tokens);
        UUID userId = UUID.randomUUID();

        server.expect(times(2), requestTo(BASE_URL + "/internal/access/users/" + userId + "/premium"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertFalse(client.isPremium(userId));
        server.verify();
    }
}
