package com.ieltspath.apigateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves Speaking practice requests against the real gateway routes in api-gateway.yaml, in file order: the first route
 * whose Path predicate matches decides the filters. Storing a recording is an upload; the other calls are ordinary ones.
 */
class LearningSpeakingUploadRouteTierTest {
    private static final String RECORDING = "/api/learning/speaking-practice/submissions/7c9e6679/recordings/3f2504e0";

    @Test
    void storingARecordingIsLimitedAsAnUploadWithTimeToSend() throws IOException {
        Map<String, Object> route = firstRouteMatching(RECORDING);

        assertThat(route.get("id")).isEqualTo("learning-speaking-recording-upload-limited");
        assertThat(tier(route)).isEqualTo("upload");
        assertThat(((Map<?, ?>) route.get("metadata")).get("response-timeout")).isEqualTo(120000);
    }

    @Test
    void theOtherSpeakingPracticeCallsStayOnTheDefaultLearningRoute() throws IOException {
        for (String path : List.of("/api/learning/speaking-practice/terms",
                "/api/learning/speaking-practice/submissions",
                "/api/learning/speaking-practice/submissions/7c9e6679",
                "/api/learning/speaking-practice/submissions/7c9e6679/complete")) {
            assertThat(firstRouteMatching(path).get("id")).as(path).isEqualTo("learning-service");
        }
    }

    private Map<String, Object> firstRouteMatching(String requestPath) throws IOException {
        AntPathMatcher matcher = new AntPathMatcher();
        return routes().stream()
                .filter(route -> pathPatterns(route).stream().anyMatch(pattern -> matcher.match(pattern, requestPath)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no route matches " + requestPath));
    }

    private static String tier(Map<String, Object> route) {
        return castList(route.get("filters")).stream()
                .map(filter -> (Map<String, Object>) filter)
                .filter(filter -> "ClientRateLimit".equals(filter.get("name")))
                .map(filter -> String.valueOf(((Map<?, ?>) filter.get("args")).get("tier")))
                .findFirst().orElseThrow();
    }

    private static List<String> pathPatterns(Map<String, Object> route) {
        return castList(route.get("predicates")).stream()
                .map(String::valueOf)
                .filter(predicate -> predicate.startsWith("Path="))
                .flatMap(predicate -> Arrays.stream(predicate.substring("Path=".length()).split(",")))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> routes() throws IOException {
        Path configPath = Path.of("..", "config-server", "config-repo", "api-gateway.yaml");
        try (InputStream input = Files.newInputStream(configPath)) {
            Map<String, Object> current = new Yaml().load(input);
            for (String key : List.of("spring", "cloud", "gateway", "server", "webflux")) {
                current = (Map<String, Object>) current.get(key);
            }
            return castList(current.get("routes"));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object value) {
        return value == null ? List.of() : (List<T>) value;
    }
}
