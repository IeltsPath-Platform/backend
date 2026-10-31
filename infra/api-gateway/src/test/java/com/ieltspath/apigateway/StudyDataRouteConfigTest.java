package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Goals, learning activity and study time are served by the Learning Service, but clients keep calling the paths the User
 * Service had. The Gateway takes the first route whose path matches, so the moved paths must come before the User Service
 * routes and rewrite to the paths of Learning.
 */
class StudyDataRouteConfigTest {
    private static final String ROUTES = "spring.cloud.gateway.server.webflux.routes[";
    private static final String GOAL = UUID.randomUUID().toString();
    private static final String SESSION = UUID.randomUUID().toString();
    private static final String LEARNER = UUID.randomUUID().toString();

    private record Route(String id, String uri, List<String> paths, Optional<String[]> rewrite) {}

    private record Resolved(String routeId, String uri, String path) {}

    private static List<Route> routes() throws IOException {
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway", new FileSystemResource(
                ServiceUrlWiringConfigTest.repoRoot().resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        List<Route> routes = new ArrayList<>();
        for (int index = 0; yaml.getProperty(ROUTES + index + "].id") != null; index++) {
            String prefix = ROUTES + index + "]";
            List<String> paths = new ArrayList<>();
            for (int p = 0; yaml.getProperty(prefix + ".predicates[" + p + "]") != null; p++) {
                String predicate = String.valueOf(yaml.getProperty(prefix + ".predicates[" + p + "]"));
                if (predicate.startsWith("Path=")) paths.addAll(List.of(predicate.substring(5).split(",")));
            }
            Optional<String[]> rewrite = Optional.empty();
            for (int f = 0; yaml.getProperty(prefix + ".filters[" + f + "]") != null; f++) {
                String filter = String.valueOf(yaml.getProperty(prefix + ".filters[" + f + "]"));
                if (filter.startsWith("RewritePath=")) {
                    String[] parts = filter.substring("RewritePath=".length()).split(",\\s*", 2);
                    rewrite = Optional.of(parts);
                }
            }
            routes.add(new Route(String.valueOf(yaml.getProperty(prefix + ".id")),
                    String.valueOf(yaml.getProperty(prefix + ".uri")), paths, rewrite));
        }
        return routes;
    }

    /** What the Gateway does with a request: the first matching route, then its rewrite. */
    private static Resolved resolve(String path) throws IOException {
        for (Route route : routes()) {
            for (String pattern : route.paths()) {
                if (new PathPatternParser().parse(pattern.trim()).matches(PathContainer.parsePath(path))) {
                    String target = route.rewrite()
                            // As the Gateway does: "$\{name}" in YAML stops the config from resolving it as a placeholder.
                            .map(parts -> Pattern.compile(parts[0]).matcher(path).replaceAll(parts[1].replace("$\\", "$")))
                            .orElse(path);
                    return new Resolved(route.id(), route.uri(), target);
                }
            }
        }
        throw new AssertionError("no route matches " + path);
    }

    private static void movesToLearning(String incoming, String expectedPath) throws IOException {
        Resolved resolved = resolve(incoming);
        assertEquals("lb://learning-service", resolved.uri(), incoming + " went to " + resolved.routeId());
        assertEquals(expectedPath, resolved.path(), incoming);
    }

    private static void staysWithUser(String incoming) throws IOException {
        Resolved resolved = resolve(incoming);
        assertEquals("lb://USER-SERVICE", resolved.uri(), incoming + " went to " + resolved.routeId());
        assertEquals(incoming, resolved.path());
    }

    @Test
    void goalsOfTheLearnerAreServedByLearningUnderTheirOldPaths() throws IOException {
        movesToLearning("/api/users/me/learning-goals", "/api/learning/goals");
        movesToLearning("/api/users/me/learning-goals/active", "/api/learning/goals/active");
        movesToLearning("/api/users/me/learning-goals/" + GOAL + "/status", "/api/learning/goals/" + GOAL + "/status");
    }

    @Test
    void goalsOfAnotherLearnerForStaffAreServedByLearning() throws IOException {
        movesToLearning("/api/users/" + LEARNER + "/learning-goals",
                "/api/learning/admin/users/" + LEARNER + "/goals");
        movesToLearning("/api/users/" + LEARNER + "/learning-goals/" + GOAL + "/status",
                "/api/learning/admin/users/" + LEARNER + "/goals/" + GOAL + "/status");
    }

    @Test
    void studySessionsAndStudyTimeStatisticsAreServedByLearning() throws IOException {
        movesToLearning("/api/users/me/study-sessions", "/api/learning/study-sessions");
        movesToLearning("/api/users/me/study-sessions/" + SESSION + "/heartbeats",
                "/api/learning/study-sessions/" + SESSION + "/heartbeats");
        movesToLearning("/api/users/me/study-sessions/" + SESSION + "/close",
                "/api/learning/study-sessions/" + SESSION + "/close");
        movesToLearning("/api/users/me/statistics/study-time", "/api/learning/statistics/study-time");
    }

    @Test
    void learningActivityIsServedByLearning() throws IOException {
        movesToLearning("/api/learning-support/activities", "/api/learning/activities");
        movesToLearning("/api/learning-support/activities/" + GOAL, "/api/learning/activities/" + GOAL);
    }

    @Test
    void everythingElseOfTheUserServiceStaysWhereItWas() throws IOException {
        staysWithUser("/auth/login");
        staysWithUser("/api/users/me");
        staysWithUser("/api/users/me/profile");
        staysWithUser("/api/users/me/avatar");
        staysWithUser("/api/users/" + LEARNER);
        staysWithUser("/api/users/" + LEARNER + "/public-profile");
        staysWithUser("/api/users/me/statistics/login-history");
    }

    @Test
    void theMovedPathsAreMatchedBeforeTheUserServiceCatchAll() throws IOException {
        List<String> ids = routes().stream().map(Route::id).toList();
        int catchAll = ids.indexOf("user-service");
        assertTrue(catchAll >= 0);
        for (String id : ids) {
            if (id.startsWith("learning-study-") && ids.indexOf(id) > catchAll) {
                throw new AssertionError(id + " comes after the user-service route and would never be reached");
            }
        }
    }
}
