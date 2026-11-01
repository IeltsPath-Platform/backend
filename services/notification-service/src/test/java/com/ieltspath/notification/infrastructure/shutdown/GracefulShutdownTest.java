package com.ieltspath.notification.infrastructure.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.env.PropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GracefulShutdownTest {
    @Test
    void configuresGracefulServerAndFastInstanceDiscovery() throws IOException {
        PropertySource<?> application = yaml("../../infra/config-server/config-repo/application.yaml");
        PropertySource<?> eureka = yaml("../../infra/config-server/config-repo/eureka-server.yaml");
        PropertySource<?> gateway = yaml("../../infra/config-server/config-repo/api-gateway.yaml");

        assertThat(application.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(application.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
        assertThat(application.getProperty("eureka.client.registry-fetch-interval-seconds")).isEqualTo(5);
        assertThat(application.getProperty("eureka.instance.lease-renewal-interval-in-seconds")).isEqualTo(10);
        assertThat(application.getProperty("eureka.instance.lease-expiration-duration-in-seconds")).isEqualTo(30);
        assertThat(eureka.getProperty("eureka.server.response-cache-update-interval-ms")).isEqualTo(3000);
        assertThat(eureka.getProperty("eureka.server.eviction-interval-timer-in-ms")).isEqualTo(5000);
        assertThat(gateway.getProperty("spring.cloud.loadbalancer.cache.ttl")).isEqualTo("5s");
    }

    @Test
    @SuppressWarnings("unchecked")
    void givesSpringServicesFortySecondsAndShortDnsCache() throws IOException {
        Map<String, Object> compose = new Yaml().load(Files.readString(Path.of("../../docker-compose.yml")));
        Map<String, Object> service = (Map<String, Object>) compose.get("x-spring-service");
        Map<String, Object> environment = (Map<String, Object>) compose.get("x-spring-env");

        assertThat(service.get("stop_grace_period")).isEqualTo("40s");
        assertThat(environment.get("JAVA_TOOL_OPTIONS").toString()).contains("-Dsun.net.inetaddr.ttl=5");
    }

    private static PropertySource<?> yaml(String path) throws IOException {
        return new YamlPropertySourceLoader().load("test", new FileSystemResource(path)).getFirst();
    }
}
