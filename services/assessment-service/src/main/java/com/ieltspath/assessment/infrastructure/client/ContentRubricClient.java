package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.infrastructure.client.GatewayRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Duration;

@Component
public class ContentRubricClient implements ActiveRubricProvider {
    private final RestClient client;
    private final AssessmentServiceTokenProvider serviceTokens;
    public ContentRubricClient(RestClient.Builder builder,
            @Value("${assessment.content.base-url:http://localhost:8082}") String baseUrl,
            AssessmentServiceTokenProvider serviceTokens) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.serviceTokens = serviceTokens;
    }
    @Override public Rubric getActive(String task) { return get("/internal/learning-content/rubrics/active?task=" + task); }
    @Override public Rubric getVersion(String code, String version) {
        return get("/internal/learning-content/rubrics/" + code + "/" + version);
    }
    private Rubric get(String path) {
        try {
            Rubric rubric = client.get().uri(path).header(HttpHeaders.AUTHORIZATION,
                    serviceTokens.authorizationHeader())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId()).retrieve().body(Rubric.class);
            if (rubric == null || rubric.criteria() == null || rubric.criteria().isEmpty())
                throw new ContentUnavailableException("Content Service returned no usable rubric");
            return rubric;
        } catch (RestClientException exception) {
            throw new ContentUnavailableException("Content Service could not provide the rubric", exception);
        }
    }
}
