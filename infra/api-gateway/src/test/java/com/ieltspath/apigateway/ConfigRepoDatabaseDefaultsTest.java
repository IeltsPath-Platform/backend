package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A service run from the IDE has no Compose override, so its default database URL must reach the one PostgreSQL that
 * Compose publishes on the host (AGENTS.md: localhost:5440).
 */
class ConfigRepoDatabaseDefaultsTest {
    private static final Pattern DB_URL = Pattern.compile("\\$\\{(\\w+_DB_URL):(jdbc:postgresql://[^}]+)}");
    private static final String HOST_POSTGRES = "jdbc:postgresql://localhost:5440/";

    @Test
    void everyServiceDefaultsToTheComposePostgresPortOnTheHost() throws IOException {
        List<String> wrong = new ArrayList<>();
        int found = 0;
        Path configRepo = ServiceUrlWiringConfigTest.repoRoot().resolve("infra/config-server/config-repo");
        try (Stream<Path> files = Files.list(configRepo)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith("-service.yaml")).toList()) {
                Matcher matcher = DB_URL.matcher(Files.readString(file));
                while (matcher.find()) {
                    found++;
                    if (!matcher.group(2).startsWith(HOST_POSTGRES)) {
                        wrong.add(file.getFileName() + " " + matcher.group(1) + " -> " + matcher.group(2));
                    }
                }
            }
        }
        assertTrue(found >= 9, "expected a default database URL in every business service, found " + found);
        assertEquals(List.of(), wrong);
    }
}
