package com.group01.assessment.infrastructure.client;

import com.group01.assessment.application.port.KnowledgeMappingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Component
public class ContentKnowledgeMappingClient implements KnowledgeMappingProvider {
    private final RestClient restClient;

    public ContentKnowledgeMappingClient(RestClient.Builder builder,
                                         @Value("${assessment.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    @Override
    public Map<UUID, List<KnowledgePointWeight>> findByQuestionVersionIds(Collection<UUID> questionVersionIds) {
        MappingResponse response = restClient.post()
                .uri("/internal/assessment-content/knowledge-point-mappings")
                .header(HttpHeaders.AUTHORIZATION, GatewayRequestContext.bearerToken())
                .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                .body(new MappingRequest(List.copyOf(questionVersionIds)))
                .retrieve()
                .body(MappingResponse.class);
        if (response == null || response.mappings() == null) {
            throw new IllegalStateException("Content Service returned an invalid knowledge-point mapping response");
        }
        Set<UUID> requested = new HashSet<>(questionVersionIds);
        Map<UUID, List<KnowledgePointWeight>> byQuestionVersion = new LinkedHashMap<>();
        for (Mapping mapping : response.mappings()) {
            if (mapping == null || mapping.questionVersionId() == null || mapping.knowledgePointId() == null
                    || mapping.weight() == null || mapping.weight().signum() < 0
                    || !requested.contains(mapping.questionVersionId())) {
                throw new IllegalStateException("Content Service returned an invalid knowledge-point mapping");
            }
            byQuestionVersion.computeIfAbsent(mapping.questionVersionId(), ignored -> new ArrayList<>())
                    .add(new KnowledgePointWeight(mapping.knowledgePointId(), mapping.weight()));
        }
        return byQuestionVersion;
    }

    private record MappingRequest(List<UUID> questionVersionIds) {}
    private record MappingResponse(List<Mapping> mappings) {}
    private record Mapping(UUID questionVersionId, UUID knowledgePointId, BigDecimal weight) {}
}
