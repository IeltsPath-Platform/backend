package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestAssessmentStudyContextClientTest {
    private final UUID source = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestAssessmentStudyContextClient client =
            new RestAssessmentStudyContextClient(builder.baseUrl("http://assessment.test").build());

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void signedIn() {
        Jwt jwt = Jwt.withTokenValue("learner-token").header("alg", "none").subject("learner").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }

    private String url() {
        return "http://assessment.test/api/assessments/me/statistics/study-context?sourceType=ASSESSMENT&sourceId=" + source;
    }

    @Test
    void readsTheCourseOfTheAssessmentAsTheCaller() {
        signedIn();
        server.expect(requestTo(url())).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer learner-token"))
                .andRespond(withSuccess("{\"sourceId\":\"" + source + "\",\"courseId\":\"" + course + "\"}",
                        MediaType.APPLICATION_JSON));

        var context = client.context(source);

        assertEquals(source, context.sourceId());
        assertEquals(course, context.courseId());
        server.verify();
    }

    @Test
    void aSourceTheAssessmentServiceDoesNotKnowOrRefusesIsNotAvailable() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN, HttpStatus.CONFLICT}) {
            server.reset();
            server.expect(requestTo(url())).andRespond(withStatus(status));

            assertEquals("SOURCE_NOT_AVAILABLE",
                    assertThrows(StudyTrackingException.class, () -> client.context(source)).getCode());
        }
    }

    @Test
    void aFailureOrAnAnswerForAnotherSourceMeansTheSourceCannotBeChecked() {
        server.expect(requestTo(url())).andRespond(withServerError());
        assertEquals("SOURCE_UNAVAILABLE",
                assertThrows(StudyTrackingException.class, () -> client.context(source)).getCode());

        server.reset();
        server.expect(requestTo(url())).andRespond(withSuccess(
                "{\"sourceId\":\"" + UUID.randomUUID() + "\",\"courseId\":null}", MediaType.APPLICATION_JSON));
        assertEquals("SOURCE_UNAVAILABLE",
                assertThrows(StudyTrackingException.class, () -> client.context(source)).getCode());
    }
}
