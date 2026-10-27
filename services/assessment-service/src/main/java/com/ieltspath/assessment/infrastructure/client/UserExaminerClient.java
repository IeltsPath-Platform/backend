package com.ieltspath.assessment.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ieltspath.assessment.application.exception.UserServiceUnavailableException;
import com.ieltspath.assessment.application.port.ExaminerDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Asks the user service about an examiner with the assessment service identity. */
@Component
public class UserExaminerClient implements ExaminerDirectory {
    private final RestClient restClient;
    private final AssessmentServiceTokenProvider serviceTokens;

    @Autowired
    public UserExaminerClient(RestClient.Builder builder,
                              @Value("${USER_SERVICE_URL:http://localhost:8085}") String userBaseUrl,
                              AssessmentServiceTokenProvider serviceTokens) {
        this(client(builder, userBaseUrl), serviceTokens);
    }

    UserExaminerClient(RestClient restClient, AssessmentServiceTokenProvider serviceTokens) {
        this.restClient = restClient;
        this.serviceTokens = serviceTokens;
    }

    private static RestClient client(RestClient.Builder builder, String userBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
    }

    @Override
    public Optional<Examiner> find(UUID examinerId) {
        ExaminerResponse response;
        try {
            response = restClient.get()
                    .uri("/internal/users/examiners/{id}", examinerId)
                    .header(HttpHeaders.AUTHORIZATION, serviceTokens.authorizationHeader())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, answer) -> { })
                    .body(ExaminerResponse.class);
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException("User Service could not describe the examiner", exception);
        }
        if (response == null || response.id() == null) return Optional.empty();
        return Optional.of(new Examiner(response.id(), response.acceptingSubmissions(), response.dailyLimit()));
    }

    @Override
    public Map<UUID, Examiner> findAll(Collection<UUID> examinerIds) {
        List<UUID> ids = examinerIds.stream().distinct().toList();
        if (ids.isEmpty()) return Map.of();
        ExaminerResponse[] response;
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/internal/users/examiners")
                            .queryParam("ids", ids.stream().map(UUID::toString).collect(Collectors.joining(","))).build())
                    .header(HttpHeaders.AUTHORIZATION, serviceTokens.authorizationHeader())
                    .header("X-Correlation-Id", GatewayRequestContext.correlationId())
                    .retrieve()
                    .body(ExaminerResponse[].class);
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException("User Service could not describe the examiners", exception);
        }
        Map<UUID, Examiner> found = new HashMap<>();
        for (ExaminerResponse item : response == null ? new ExaminerResponse[0] : response) {
            if (item != null && item.id() != null) {
                found.put(item.id(), new Examiner(item.id(), item.acceptingSubmissions(), item.dailyLimit()));
            }
        }
        return found;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExaminerResponse(UUID id, boolean acceptingSubmissions, int dailyLimit) {}
}
