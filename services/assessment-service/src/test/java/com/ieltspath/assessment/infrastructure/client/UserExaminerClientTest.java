package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserExaminerClientTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://user");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AssessmentServiceTokenProvider tokens = mock(AssessmentServiceTokenProvider.class);
    private final UserExaminerClient client = new UserExaminerClient(builder.build(), tokens);
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    UserExaminerClientTest() {
        when(tokens.authorizationHeader()).thenReturn("Bearer service-token");
    }

    @Test
    void asksOnceForTheWholeListAndLeavesOutWhoIsNotAnActiveExaminer() {
        server.expect(once(), requestTo("http://user/internal/users/examiners?ids=" + first + "," + second))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer service-token"))
                .andRespond(withSuccess("[{\"id\":\"" + first + "\",\"acceptingSubmissions\":true,\"dailyLimit\":30,\"extra\":1}]",
                        MediaType.APPLICATION_JSON));

        var found = client.findAll(List.of(first, second, first));

        assertEquals(1, found.size());
        assertEquals(30, found.get(first).dailyLimit());
        assertTrue(found.get(first).acceptingSubmissions());
        server.verify();
    }

    @Test
    void makesNoCallForAnEmptyList() {
        assertTrue(client.findAll(List.of()).isEmpty());
        server.verify();
    }

    @Test
    void anUnreachableOrFailingUserServiceIsReportedAsUnavailable() {
        server.expect(once(), requestTo("http://user/internal/users/examiners?ids=" + first)).andRespond(withServerError());
        assertThrows(UserServiceUnavailableException.class, () -> client.findAll(List.of(first)));

        server.reset();
        server.expect(once(), requestTo("http://user/internal/users/examiners?ids=" + second))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThrows(UserServiceUnavailableException.class, () -> client.findAll(List.of(second)));
    }
}
