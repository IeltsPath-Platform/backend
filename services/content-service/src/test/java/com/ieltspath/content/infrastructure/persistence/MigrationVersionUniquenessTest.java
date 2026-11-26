package com.ieltspath.content.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationVersionUniquenessTest {
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @Test
    void everyMigrationHasAUniqueVersion() throws IOException, URISyntaxException {
        var location = getClass().getClassLoader().getResource("db/migration");
        assertNotNull(location, "Content-service migration directory must be on the test classpath");

        Map<String, String> migrationByVersion = new HashMap<>();
        try (var files = Files.list(Path.of(location.toURI()))) {
            for (Path migration : files.filter(path -> path.getFileName().toString().endsWith(".sql")).toList()) {
                String filename = migration.getFileName().toString();
                Matcher matcher = VERSIONED_MIGRATION.matcher(filename);
                assertTrue(matcher.matches(), "Migration filename must include a version: " + filename);
                String version = matcher.group(1);
                String previous = migrationByVersion.putIfAbsent(version, filename);
                assertNull(previous, () -> "Migration version " + version + " is used by both " + previous
                        + " and " + filename);
            }
        }
    }
}
