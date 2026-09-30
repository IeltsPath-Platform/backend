package com.group01.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LearningSupportRouteConfigTest {
    @Test
    void activityAndStreakRouteToUserBeforeLearningSupportFallback() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        String prefix = "spring.cloud.gateway.server.webflux.routes[";
        int userIndex = -1;
        int fallbackIndex = -1;
        for (int index = 0; index < 30; index++) {
            Object id = yaml.getProperty(prefix + index + "].id");
            if ("user-learning-support-service".equals(id)) userIndex = index;
            if ("learning-support-service".equals(id)) fallbackIndex = index;
        }
        assertTrue(userIndex >= 0 && userIndex < fallbackIndex);
        assertEquals("lb://USER-SERVICE", yaml.getProperty(prefix + userIndex + "].uri"));
        assertEquals("Path=/api/learning-support/activities/**,/api/learning-support/streak/**",
                yaml.getProperty(prefix + userIndex + "].predicates[0]"));
        assertEquals("Path=/api/learning-support/**", yaml.getProperty(prefix + fallbackIndex + "].predicates[0]"));
    }
}
