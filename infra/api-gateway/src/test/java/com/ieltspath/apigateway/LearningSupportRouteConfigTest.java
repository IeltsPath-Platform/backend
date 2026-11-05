package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LearningSupportRouteConfigTest {
    @Test
    void learningSupportHasNoGeneralFallbackRoute() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        String prefix = "spring.cloud.gateway.server.webflux.routes[";
        for (int index = 0; index < 40; index++) {
            assertNotEquals("Path=/api/learning-support/**", yaml.getProperty(prefix + index + "].predicates[0]"));
        }
    }
}
