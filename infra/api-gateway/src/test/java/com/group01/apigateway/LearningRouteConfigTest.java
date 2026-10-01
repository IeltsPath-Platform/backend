package com.group01.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LearningRouteConfigTest {
    @Test
    void learningUsesDiscoveryAndInternalJwt() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
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
}
