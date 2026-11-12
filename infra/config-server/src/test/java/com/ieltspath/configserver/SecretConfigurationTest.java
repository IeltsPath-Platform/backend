package com.ieltspath.configserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SecretConfigurationTest {
    private static final Pattern FALLBACK = Pattern.compile(
            "\\$\\{([A-Z0-9_]*(?:_DB_PASSWORD|JWT_SECRET|RABBITMQ_PASSWORD|REDIS_PASSWORD)):");

    @Test
    void requiredSecretsHaveNoFallbackInTheConfigRepository() throws Exception {
        try (var files = Files.list(Path.of("config-repo"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yaml")).toList()) {
                var matcher = FALLBACK.matcher(Files.readString(file));
                while (matcher.find()) {
                    throw new AssertionError(file.getFileName() + ": " + matcher.group(1) + " has a fallback");
                }
            }
        }
    }

    @Test
    void missingInternalJwtSecretPreventsContextStartupAndNamesTheVariable() throws Exception {
        String yaml = Files.readString(Path.of("config-repo/content-service.yaml"));
        var matcher = Pattern.compile("internal-jwt-secret:\\s*(\\$\\{[^}]+})").matcher(yaml);
        assertThat(matcher.find()).isTrue();
        new ApplicationContextRunner()
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    context.getEnvironment().getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                })
                .withPropertyValues("app.auth.internal-jwt-secret=" + matcher.group(1))
                .withUserConfiguration(SecretConsumer.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("GATEWAY_INTERNAL_JWT_SECRET");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class SecretConsumer {
        @Bean
        static org.springframework.context.support.PropertySourcesPlaceholderConfigurer placeholders() {
            return new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        Object configuredSecret(@Value("${app.auth.internal-jwt-secret}") String secret) {
            return new Object();
        }
    }
}
