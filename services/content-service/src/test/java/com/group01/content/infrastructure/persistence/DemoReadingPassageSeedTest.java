package com.group01.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DemoReadingPassageSeedTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void demoReadingSectionOfAPublishedPracticeSetHasOnePassage() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             ResultSet rows = connection.createStatement().executeQuery(
                     "SELECT p.package_type, p.status, s.skill, a.asset_type, a.text_content "
                             + "FROM content_asset_links l "
                             + "JOIN content_assets a ON a.id = l.asset_id "
                             + "JOIN content_sections s ON s.id = l.section_id "
                             + "JOIN content_package_versions pv ON pv.id = s.package_version_id "
                             + "JOIN content_packages p ON p.id = pv.package_id AND p.current_published_version_id = pv.id "
                             + "WHERE p.code = 'DEMO_MAIN_FLOW_READING'")) {
            assertTrue(rows.next());
            assertEquals("PRACTICE_SET", rows.getString(1));
            assertEquals("PUBLISHED", rows.getString(2));
            assertEquals("READING", rows.getString(3));
            assertEquals("PASSAGE", rows.getString(4));
            // Five paragraphs separated by blank lines.
            assertEquals(5, rows.getString(5).split("\\R\\s*\\R").length);
            assertTrue(!rows.next(), "the demo section has exactly one passage");
        }
    }
}
