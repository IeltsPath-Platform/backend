package com.ieltspath.apigateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRouteConsistencyTest {
    private static final Set<String> AUTH_ENDPOINTS = Set.of(
            "/auth/login",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/google",
            "/auth/google/nonce",
            "/auth/verify-email",
            "/auth/resend-verification",
            "/api/users/register");

    @Test
    void rateLimitedRouteContainsExactlyThePublicAuthEndpoints() throws IOException {
        Map<String, Object> config = loadConfig();
        List<Map<String, Object>> routes = nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes");
        Map<String, Object> authRoute = routes.stream()
                .filter(route -> "user-auth-limited".equals(route.get("id")))
                .findFirst()
                .orElseThrow();
        Set<String> configuredPaths = routePaths(authRoute);

        assertThat(configuredPaths).containsExactlyInAnyOrderElementsOf(AUTH_ENDPOINTS);
    }

    @Test
    void everyPublicAuthEndpointExceptLogoutHasAnAuthLimitedRoute() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> gatewaySecurity = nested(config, "gateway", "security");
        List<String> publicEndpoints = castList(gatewaySecurity.get("public-endpoints"));
        Set<String> expected = publicEndpoints.stream()
                .filter(path -> path.startsWith("/auth/") && !path.equals("/auth/logout"))
                .filter(path -> !path.endsWith("/**"))
                .collect(Collectors.toSet());
        expected.add("/api/users/register");
        expected.remove("/auth/refresh");
        List<Map<String, Object>> routes = nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes");
        Map<String, Object> authRoute = routes.stream()
                .filter(route -> "user-auth-limited".equals(route.get("id")))
                .findFirst()
                .orElseThrow();
        Set<String> actual = routePaths(authRoute);

        assertThat(actual).containsAll(expected);
    }

    @Test
    void authenticatedReauthEndpointUsesTheAuthRateLimitTier() throws IOException {
        Map<String, Object> config = loadConfig();
        List<Map<String, Object>> routes = nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes");
        Map<String, Object> route = routes.stream().filter(item -> "user-reauth-limited".equals(item.get("id")))
                .findFirst().orElseThrow();
        assertThat(routePaths(route)).containsExactly("/api/users/me/reauth");
        assertThat(route.toString()).contains("tier=auth");
    }

    @Test
    void refreshUsesTheSeparateLargerBurstTier() throws IOException {
        Map<String, Object> config = loadConfig();
        List<Map<String, Object>> routes = nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes");
        Map<String, Object> route = routes.stream().filter(item -> "user-refresh-limited".equals(item.get("id")))
                .findFirst().orElseThrow();
        assertThat(routePaths(route)).containsExactly("/auth/refresh");
        assertThat(route.toString()).contains("auth-refresh");
    }

    @Test
    void sectionQuestionGenerationUsesTheAiTierOnlyForPost() throws IOException {
        Map<String, Object> config = loadConfig();
        List<Map<String, Object>> routes = nested(config, "spring", "cloud", "gateway", "server", "webflux", "routes");
        Map<String, Object> route = routes.stream().filter(item -> "content-ai-drafts-limited".equals(item.get("id")))
                .findFirst().orElseThrow();
        Set<String> paths = routePaths(route);
        assertThat(paths).contains("/api/content/admin/ai-drafts/section-questions");
        assertThat(paths).doesNotContain("/api/content/admin/ai-drafts/section-questions/open");
        assertThat(castList(route.get("predicates"))).contains("Method=POST");
        Object filter = ((List<?>) route.get("filters")).get(0);
        assertThat(castMap(filter).get("args").toString()).contains("ai");
    }

    private Map<String, Object> loadConfig() throws IOException {
        Path configPath = Path.of("..", "config-server", "config-repo", "api-gateway.yaml");
        try (InputStream input = Files.newInputStream(configPath)) {
            return new Yaml().load(input);
        }
    }

    private Set<String> routePaths(Map<String, Object> route) {
        List<String> predicates = castList(route.get("predicates"));
        return predicates.stream()
                .filter(predicate -> predicate.startsWith("Path="))
                .flatMap(predicate -> Arrays.stream(predicate.substring("Path=".length()).split(",")))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private <T> T nested(Map<String, Object> map, String... path) {
        Object value = map;
        for (String key : path) value = ((Map<String, Object>) value).get(key);
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        if (value instanceof String string) return List.of(string);
        return (List<String>) value;
    }
}
