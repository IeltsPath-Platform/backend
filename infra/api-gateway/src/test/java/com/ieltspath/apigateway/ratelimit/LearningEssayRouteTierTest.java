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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves a Learning essay submission against the real gateway routes in api-gateway.yaml, in file order, the way the
 * gateway does: the first route whose Path predicate matches is the one whose filters apply.
 */
class LearningEssayRouteTierTest {
    private static final String ESSAY_SUBMISSION = "/api/learning/lessons/7c9e6679/essays/3f2504e0/submissions";

    @Test
    void theFirstRouteForAnEssaySubmissionLimitsItAsAi() throws IOException {
        Map<String, Object> route = firstRouteMatching(ESSAY_SUBMISSION);

        assertThat(route.get("id")).isEqualTo("learning-ai-writing-limited");
        assertThat(clientRateLimitTier(route)).contains("ai");
    }

    @Test
    void theDefaultTierIsDeclaredOnlyAsADefaultFilter() throws IOException {
        Map<String, Object> config = load();
        List<Map<String, Object>> defaults = castList(nested(config, "spring", "cloud", "gateway", "server", "webflux", "default-filters"));

        assertThat(defaults).anySatisfy(filter -> {
            assertThat(filter.get("name")).isEqualTo("ClientRateLimit");
            assertThat(((Map<?, ?>) filter.get("args")).get("tier")).isEqualTo("default");
        });
    }

    private Map<String, Object> firstRouteMatching(String requestPath) throws IOException {
        AntPathMatcher matcher = new AntPathMatcher();
        return routes(load()).stream()
                .filter(route -> pathPatterns(route).stream().anyMatch(pattern -> matcher.match(pattern, requestPath)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no route matches " + requestPath));
    }

    private static Optional<String> clientRateLimitTier(Map<String, Object> route) {
        List<Map<String, Object>> filters = castList(route.get("filters"));
        return filters.stream()
                .filter(filter -> "ClientRateLimit".equals(filter.get("name")))
                .map(filter -> String.valueOf(((Map<?, ?>) filter.get("args")).get("tier")))
                .findFirst();
    }

    private static List<String> pathPatterns(Map<String, Object> route) {
        return castList(route.get("predicates")).stream()
                .map(String::valueOf)
                .filter(predicate -> predicate.startsWith("Path="))
                .flatMap(predicate -> Arrays.stream(predicate.substring("Path=".length()).split(",")))
                .toList();
    }

    private static List<Map<String, Object>> routes(Map<String, Object> config) {
        return castList(nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes"));
    }

    private static Map<String, Object> load() throws IOException {
        Path configPath = Path.of("..", "config-server", "config-repo", "api-gateway.yaml");
        try (InputStream input = Files.newInputStream(configPath)) {
            return new Yaml().load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String... keys) {
        Object current = root;
        for (String key : keys) {
            current = ((Map<String, Object>) current).get(key);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object value) {
        return value == null ? List.of() : (List<T>) value;
    }
}
