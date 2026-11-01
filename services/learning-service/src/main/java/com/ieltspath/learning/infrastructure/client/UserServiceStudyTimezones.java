package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.port.StudyTimezones;
import com.ieltspath.learning.domain.repository.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * The timezone on the learner's profile, read from the User Service with the caller's token. When it cannot be read the
 * last timezone Learning recorded for the learner is used, then {@code Asia/Ho_Chi_Minh}, so study tracking never stops
 * because the profile is out of reach.
 */
@Component
public class UserServiceStudyTimezones implements StudyTimezones {
    private static final Logger log = LoggerFactory.getLogger(UserServiceStudyTimezones.class);
    private static final ZoneId DEFAULT = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RestClient restClient;
    private final StudySessionRepository sessions;

    @Autowired
    public UserServiceStudyTimezones(RestClient.Builder builder,
            @Value("${learning.user.base-url:http://localhost:8085}") String userBaseUrl,
            StudySessionRepository sessions) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
        this.sessions = sessions;
    }

    UserServiceStudyTimezones(RestClient restClient, StudySessionRepository sessions) {
        this.restClient = restClient;
        this.sessions = sessions;
    }

    @Override
    public ZoneId zoneOf(UUID userId) {
        return fromProfile(userId)
                .or(() -> sessions.latestTimezone(userId).flatMap(UserServiceStudyTimezones::parse))
                .orElse(DEFAULT);
    }

    private Optional<ZoneId> fromProfile(UUID userId) {
        Optional<String> bearer = GatewayRequestHeaders.bearerToken();
        if (bearer.isEmpty()) return Optional.empty();
        try {
            Answer answer = restClient.get().uri("/internal/users/{userId}/study-timezone", userId)
                    .headers((HttpHeaders headers) -> {
                        headers.setBearerAuth(bearer.get());
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    }).retrieve().body(Answer.class);
            return answer == null ? Optional.empty() : parse(answer.timezone());
        } catch (RuntimeException failure) {
            log.warn("Study timezone unavailable from the User Service: userId={}", userId, failure);
            return Optional.empty();
        }
    }

    private static Optional<ZoneId> parse(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try {
            return Optional.of(ZoneId.of(name));
        } catch (DateTimeException invalid) {
            return Optional.empty();
        }
    }

    record Answer(String timezone) {}
}
