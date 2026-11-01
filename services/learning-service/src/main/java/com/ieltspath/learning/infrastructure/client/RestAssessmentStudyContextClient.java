package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.port.AssessmentStudyContext;
import com.ieltspath.learning.application.port.StudySourceVerifier.Context;
import com.ieltspath.learning.domain.exception.StudyTrackingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

/** Asks the Assessment Service whether the signed-in learner may log study time against an assessment. */
@Component
public class RestAssessmentStudyContextClient implements AssessmentStudyContext {
    private final RestClient restClient;

    @Autowired
    public RestAssessmentStudyContextClient(RestClient.Builder builder,
            @Value("${learning.assessment.base-url:http://localhost:8083}") String assessmentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        restClient = builder.requestFactory(requestFactory).baseUrl(assessmentBaseUrl).build();
    }

    RestAssessmentStudyContextClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Context context(UUID assessmentId) {
        try {
            Context context = restClient.get().uri(uri -> uri.path("/api/assessments/me/statistics/study-context")
                            .queryParam("sourceType", "ASSESSMENT").queryParam("sourceId", assessmentId).build())
                    .headers((HttpHeaders headers) -> {
                        GatewayRequestHeaders.bearerToken().ifPresent(headers::setBearerAuth);
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    }).retrieve().body(Context.class);
            if (context == null || !assessmentId.equals(context.sourceId())) {
                throw new StudyTrackingException("SOURCE_UNAVAILABLE");
            }
            return context;
        } catch (HttpClientErrorException refused) {
            int status = refused.getStatusCode().value();
            throw new StudyTrackingException(status == 404 || status == 403 || status == 409
                    ? "SOURCE_NOT_AVAILABLE" : "SOURCE_UNAVAILABLE");
        } catch (RestClientException unreachable) {
            throw new StudyTrackingException("SOURCE_UNAVAILABLE");
        }
    }
}
