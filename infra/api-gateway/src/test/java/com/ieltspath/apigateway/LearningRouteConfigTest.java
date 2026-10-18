package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearningRouteConfigTest {
    private static final List<String> NEW_LEARNING_PATHS = List.of(
            "/api/learning/diagnostics/00000000-0000-0000-0000-000000000000",
            "/api/learning/review-basket/start",
            "/api/learning/suggestions",
            "/api/learning/courses/00000000-0000-0000-0000-000000000000/score",
            "/api/learning/admin/score-config",
            "/api/learning/admin/learners/00000000-0000-0000-0000-000000000000/report",
            "/api/learning/writing-submissions/00000000-0000-0000-0000-000000000000/revisions",
            "/api/learning/writing-submissions/00000000-0000-0000-0000-000000000000/history"
    );

    private static final List<String> NEW_ASSESSMENT_PATHS = List.of(
            "/api/assessments/qa-reviews/queue",
            "/api/assessments/admin/examiner-quality",
            "/api/assessments/attempts/00000000-0000-0000-0000-000000000000/items/00000000-0000-0000-0000-000000000000/human-grading"
    );

    @Test
    void learningUsesDiscoveryAndInternalJwt() throws Exception {
        PropertySource<?> yaml = gatewayYaml();
        int learningIndex = -1;
        boolean hasJwtPrefix = false;
        for (int index = 0; index < 30; index++) {
            String route = "spring.cloud.gateway.server.webflux.routes[" + index + "]";
            Object id = yaml.getProperty(route + ".id");
            if ("learning-service".equals(id)) learningIndex = index;
            Object prefix = yaml.getProperty("app.auth.internal-jwt-paths[" + index + "]");
            if ("/api/learning".equals(prefix)) hasJwtPrefix = true;
        }
        assertTrue(learningIndex >= 0);
        String route = "spring.cloud.gateway.server.webflux.routes[" + learningIndex + "]";
        assertEquals("lb://learning-service", yaml.getProperty(route + ".uri"));
        assertEquals("Path=/api/learning/**", yaml.getProperty(route + ".predicates[0]"));
        assertTrue(hasJwtPrefix);
    }

    @Test
    void newLearningAndAssessmentRoutesUseInternalJwt() throws Exception {
        PropertySource<?> yaml = gatewayYaml();
        String routes = "spring.cloud.gateway.server.webflux.routes[";
        boolean learningRoute = false;
        boolean assessmentRoute = false;
        for (int index = 0; index < 30; index++) {
            Object id = yaml.getProperty(routes + index + "].id");
            Object predicate = yaml.getProperty(routes + index + "].predicates[0]");
            if ("learning-service".equals(id)
                    && "Path=/api/learning/**".equals(predicate)) learningRoute = true;
            if ("assessment-service".equals(id)
                    && "Path=/api/assessments/**".equals(predicate)) assessmentRoute = true;
        }
        assertTrue(learningRoute, "All new learning endpoints must match the learning service route");
        assertTrue(assessmentRoute, "All new assessment endpoints must match the assessment service route");

        List<String> internalJwtPaths = new java.util.ArrayList<>();
        for (int index = 0; index < 30; index++) {
            Object path = yaml.getProperty("app.auth.internal-jwt-paths[" + index + "]");
            if (path instanceof String value) internalJwtPaths.add(value);
        }
        assertTrue(internalJwtPaths.contains("/api/learning"));
        assertTrue(internalJwtPaths.contains("/api/assessments"));
        NEW_LEARNING_PATHS.forEach(path -> assertTrue(hasProtectedPrefix(path, internalJwtPaths), path));
        NEW_ASSESSMENT_PATHS.forEach(path -> assertTrue(hasProtectedPrefix(path, internalJwtPaths), path));
    }

    private static boolean hasProtectedPrefix(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private static PropertySource<?> gatewayYaml() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        return new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
    }
}
