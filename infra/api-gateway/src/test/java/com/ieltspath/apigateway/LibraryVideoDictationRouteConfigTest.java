package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LibraryVideoDictationRouteConfigTest {
    @Test
    void videoDictationRoutesToLibraryWithoutGeneralFallback() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root);
        PropertySource<?> yaml = new YamlPropertySourceLoader().load("gateway",
                new FileSystemResource(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))).getFirst();
        String prefix = "spring.cloud.gateway.server.webflux.routes[";
        int index = -1;
        for (int i = 0; i < 30; i++) {
            if ("library-video-dictation-service".equals(yaml.getProperty(prefix + i + "].id"))) index = i;
            assertNotEquals("Path=/api/learning-support/**", yaml.getProperty(prefix + i + "].predicates[0]"));
        }
        assertTrue(index >= 0);
        assertEquals("lb://LIBRARY-SERVICE", yaml.getProperty(prefix + index + "].uri"));
        assertEquals("Path=/api/learning-support/video-dictation/**", yaml.getProperty(prefix + index + "].predicates[0]"));
    }
}
