package com.ieltspath.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the URL each service uses to reach another service wired in the shared config and in Compose. */
class ServiceUrlWiringConfigTest {
    private static final Pattern SERVICE_HEADER = Pattern.compile("(?m)^  ([a-z-]+):\\s*$");

    @Test
    void contentServiceOwnsItsLibraryUrlInsteadOfBorrowingLibrarysContentUrl() throws Exception {
        assertEquals("${LIBRARY_SERVICE_URL:http://localhost:8081}",
                configRepoYaml("content-service").getProperty("content.library.base-url"));
    }

    @Test
    void composeTellsContentServiceWhereLibraryRuns() throws Exception {
        assertTrue(composeBlock("content-service").contains("LIBRARY_SERVICE_URL: http://library-service:8081"),
                "content-service needs LIBRARY_SERVICE_URL to call the library vocabulary API");
    }

    @Test
    void libraryHasNoUrlForContent() throws Exception {
        assertNull(configRepoYaml("library-service").getProperty("library.content.base-url"));
        assertFalse(composeBlock("library-service").contains("CONTENT_SERVICE_URL"),
                "library-service must not be told where content-service runs");
    }

    static Path repoRoot() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/api-gateway.yaml"))) {
            root = root.getParent();
        }
        assertNotNull(root, "repository root not found");
        return root;
    }

    static PropertySource<?> configRepoYaml(String service) throws IOException {
        return new YamlPropertySourceLoader().load(service,
                new FileSystemResource(repoRoot().resolve("infra/config-server/config-repo/" + service + ".yaml"))).getFirst();
    }

    /** The text of one service entry in docker-compose.yml, from its header line to the next service header. */
    static String composeBlock(String service) throws IOException {
        String compose = Files.readString(repoRoot().resolve("docker-compose.yml"));
        Matcher headers = SERVICE_HEADER.matcher(compose);
        int start = -1;
        int end = compose.length();
        while (headers.find()) {
            if (start >= 0) { end = headers.start(); break; }
            if (headers.group(1).equals(service)) start = headers.start();
        }
        assertTrue(start >= 0, "service " + service + " not found in docker-compose.yml");
        return compose.substring(start, end);
    }
}
