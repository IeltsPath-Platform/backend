package com.ieltspath.user.infrastructure.client;

import com.ieltspath.user.domain.exception.StudyTrackingException;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpStudySourceVerifierTest {
    final HttpStudySourceVerifier verifier=new HttpStudySourceVerifier("http://learning.test","http://assessment.test");
    MockRestServiceServer server;
    @BeforeEach void setup() {
        var builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(verifier,"client",builder.build());
        var request=new MockHttpServletRequest();
        request.addHeader("Authorization","Bearer test-request-token");
        request.addHeader("X-Correlation-Id","study-test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }
    @Test void forwardsOnlyRequestIdentityAndCorrelationAndTrustsOwnerCourse() {
        UUID source=UUID.randomUUID(),course=UUID.randomUUID();
        server.expect(requestTo("http://learning.test/api/learning/statistics/study-context?sourceType=LESSON&sourceId="+source))
                .andExpect(header("Authorization","Bearer test-request-token")).andExpect(header("X-Correlation-Id","study-test"))
                .andRespond(withSuccess("{\"sourceId\":\""+source+"\",\"courseId\":\""+course+"\",\"sourceType\":\"LESSON\"}",MediaType.APPLICATION_JSON));
        assertEquals(course,verifier.verify("LESSON",source).courseId());
        server.verify();
    }
    @Test void ownerFailureDoesNotBecomeAcceptedSource() {
        UUID source=UUID.randomUUID();
        server.expect(anything()).andRespond(withServerError());
        assertEquals("SOURCE_UNAVAILABLE",assertThrows(StudyTrackingException.class,()->verifier.verify("ASSESSMENT",source)).getCode());
    }
    @Test void mismatchedSourceAndUnknownOwnerAreRejected() {
        UUID source=UUID.randomUUID();
        server.expect(anything()).andRespond(withSuccess("{\"sourceId\":\""+UUID.randomUUID()+"\",\"courseId\":null}",MediaType.APPLICATION_JSON));
        assertThrows(StudyTrackingException.class,()->verifier.verify("REVIEW",source));
        assertThrows(IllegalArgumentException.class,()->verifier.verify("VOCABULARY_REVIEW",source));
        assertThrows(IllegalArgumentException.class,()->verifier.verify("DEMO",source));
    }
}
