package com.group01.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LibraryRouteConfigTest {
    @Test
    void personalLibraryRoutePrecedesGeneralLearningSupportRoute() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        String prefix = "spring.cloud.gateway.server.webflux.routes[";
        int personalIndex = -1;
        int generalIndex = -1;
        for (int index = 0; index < 30; index++) {
            Object id = yaml.getProperty(prefix + index + "].id");
            if ("library-personal-service".equals(id)) personalIndex = index;
            if ("learning-support-service".equals(id)) generalIndex = index;
        }
        assertTrue(personalIndex >= 0 && personalIndex < generalIndex);
        assertEquals("lb://LIBRARY-SERVICE", yaml.getProperty(prefix + personalIndex + "].uri"));
        assertEquals("Path=/api/learning-support/flashcards/**,/api/learning-support/decks/**,"
                        + "/api/learning-support/notes/**,/api/learning-support/video-progress/**,"
                        + "/api/learning-support/saved-segments/**",
                yaml.getProperty(prefix + personalIndex + "].predicates[0]"));
        assertEquals("Path=/api/learning-support/**", yaml.getProperty(prefix + generalIndex + "].predicates[0]"));
        boolean hasJwtPrefix = false;
        for (int index = 0; index < 30; index++) {
            if ("/api/learning-support".equals(yaml.getProperty("app.auth.internal-jwt-paths[" + index + "]"))) {
                hasJwtPrefix = true;
            }
        }
        assertTrue(hasJwtPrefix);
    }

    @Test
    void catalogRoutePrecedesGeneralContentRoute() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        String prefix = "spring.cloud.gateway.server.webflux.routes[";
        int libraryIndex = -1;
        int contentIndex = -1;
        for (int index = 0; index < 30; index++) {
            Object id = yaml.getProperty(prefix + index + "].id");
            if ("library-service".equals(id)) libraryIndex = index;
            if ("content-service".equals(id)) contentIndex = index;
        }
        assertTrue(libraryIndex >= 0 && libraryIndex < contentIndex);
        assertEquals("lb://LIBRARY-SERVICE", yaml.getProperty(prefix + libraryIndex + "].uri"));
        assertEquals("Path=/api/content/videos/**,/api/content/vocabulary/**,/api/content/admin/vocabulary/**",
                yaml.getProperty(prefix + libraryIndex + "].predicates[0]"));
        assertEquals("Path=/api/content/**", yaml.getProperty(prefix + contentIndex + "].predicates[0]"));
        boolean hasContentJwtPrefix = false;
        for (int index = 0; index < 30; index++) {
            if ("/api/content".equals(yaml.getProperty("app.auth.internal-jwt-paths[" + index + "]"))) {
                hasContentJwtPrefix = true;
            }
        }
        assertTrue(hasContentJwtPrefix);
    }
}
