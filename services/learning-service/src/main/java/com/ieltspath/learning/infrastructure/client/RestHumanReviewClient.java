package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.HumanReviewClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Calls Access (credits) and Assessment (reviews) with the caller's gateway bearer token. Essay text is never logged. */
@Component
public class RestHumanReviewClient implements HumanReviewClient {
    private final RestClient access;
    private final RestClient assessment;

    @Autowired
    public RestHumanReviewClient(RestClient.Builder builder,
                                 @Value("${learning.access.base-url:http://localhost:8084}") String accessBaseUrl,
                                 @Value("${learning.assessment.base-url:http://localhost:8083}") String assessmentBaseUrl) {
        this.access = client(builder, accessBaseUrl);
        this.assessment = client(builder, assessmentBaseUrl);
    }

    RestHumanReviewClient(RestClient access, RestClient assessment) {
        this.access = access;
        this.assessment = assessment;
    }

    private static RestClient client(RestClient.Builder builder, String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.clone().requestFactory(factory).baseUrl(baseUrl).build();
    }

    @Override
    public CreditCharge consumeCredit(UUID submissionId) {
        Credits credits = call(() -> access.post().uri("/internal/access/human-grading/consume")
                .headers(this::forward).body(Map.of("submissionId", submissionId)).retrieve().body(Credits.class), false);
        return new CreditCharge(credits.remainingCredits(), Boolean.TRUE.equals(credits.charged()));
    }

    @Override
    public void refundCredit(UUID submissionId) {
        call(() -> access.post().uri("/internal/access/human-grading/refund")
                .headers(this::forward).body(Map.of("submissionId", submissionId)).retrieve().body(Credits.class), false);
    }

    @Override
    public UUID createReview(CreateReview request) {
        Created created = call(() -> assessment.post().uri("/internal/assessments/human-reviews")
                .headers(this::forward).body(body(request)).retrieve().body(Created.class), true);
        if (created.reviewSubmissionId() == null) throw unavailable();
        return created.reviewSubmissionId();
    }

    @Override
    public Review fetchReview(UUID reviewSubmissionId, UUID learnerId) {
        return call(() -> assessment.get().uri("/internal/assessments/human-reviews/{id}", reviewSubmissionId)
                .headers(this::forward).retrieve().body(Review.class), false);
    }

    @Override
    public Review cancelReview(UUID reviewSubmissionId) {
        return call(() -> assessment.post().uri("/internal/assessments/human-reviews/{id}/cancel", reviewSubmissionId)
                .headers(this::forward).retrieve().body(Review.class), false);
    }

    private static Map<String, Object> body(CreateReview request) {
        Map<String, Object> rubric = new java.util.LinkedHashMap<>();
        rubric.put("code", request.rubricCode());
        rubric.put("version", request.rubricVersion());
        rubric.put("criteria", java.util.List.of());
        Map<String, Object> prompt = new java.util.LinkedHashMap<>();
        prompt.put("stem", request.promptStem());
        // The chart of a Task 1 prompt, so the examiner sees what the learner saw.
        if (!request.images().isEmpty()) {
            prompt.put("images", request.images().stream()
                    .map(image -> image.altText() == null
                            ? Map.of("url", image.url())
                            : Map.of("url", image.url(), "altText", image.altText()))
                    .toList());
        }
        return Map.of("requestId", request.requestId(), "learnerId", request.learnerId(),
                "sourceSubmissionId", request.sourceSubmissionId(), "source", request.source(),
                "task", request.task(), "promptSnapshot", prompt,
                "essay", request.essay(), "rubric", rubric, "examinerId", request.examinerId());
    }

    /** {@code refusalIsDefinite}: a 4xx answer to a create call means nothing was created, unlike a timeout or a 5xx. */
    private <T> T call(Supplier<T> request, boolean refusalIsDefinite) {
        try {
            T body = request.get();
            if (body == null) throw unavailable();
            return body;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 402) {
                throw new LearningRequestException(402, "HUMAN_GRADING_CREDIT_REQUIRED",
                        "No human-grading credit is left");
            }
            if (status == 409) {
                if ("GRADING_CANCEL_NOT_ALLOWED".equals(errorCode(exception))) {
                    throw new LearningRequestException(409, "REVIEW_CANCEL_NOT_ALLOWED",
                            "The request can be cancelled only when it is late and the examiner can no longer be changed");
                }
                if (refusalIsDefinite && "EXAMINER_UNAVAILABLE".equals(errorCode(exception))) {
                    throw new LearningRequestException(409, "EXAMINER_UNAVAILABLE",
                            "The chosen examiner cannot take this essay; choose another examiner");
                }
                throw new LearningRequestException(409, "HUMAN_GRADING_CONFLICT", "The review request conflicts");
            }
            if (refusalIsDefinite && status >= 400 && status < 500 && status != 401) {
                throw new LearningRequestException(502, "HUMAN_REVIEW_REJECTED", "The Mentor review was refused");
            }
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static String errorCode(RestClientResponseException exception) {
        try {
            ErrorBody body = exception.getResponseBodyAs(ErrorBody.class);
            return body == null ? null : body.code();
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    private void forward(HttpHeaders headers) {
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken().orElseThrow(
                () -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private static LearningRequestException unavailable() {
        return new LearningRequestException(503, "HUMAN_REVIEW_UNAVAILABLE", "Mentor review is unavailable");
    }

    private record Credits(int remainingCredits, Boolean charged) {}
    private record Created(UUID reviewSubmissionId) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorBody(String code) {}
}
