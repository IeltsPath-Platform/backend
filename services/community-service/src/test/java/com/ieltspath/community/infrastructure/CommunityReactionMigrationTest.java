package com.ieltspath.community.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class CommunityReactionMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void archivesDisplacedReactionsAndKeepsTheNewestReaction() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .target("1")
                .load()
                .migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            UUID post = UUID.randomUUID();
            UUID author = UUID.randomUUID();
            UUID learner = UUID.randomUUID();
            statement.execute("INSERT INTO posts(id,author_id,category,body,status,created_at,updated_at) "
                    + "VALUES ('" + post + "','" + author + "','GENERAL','body','ACTIVE',now(),now())");
            statement.execute("INSERT INTO post_reactions(post_id,user_id,reaction_type,created_at) VALUES "
                    + "('" + post + "','" + learner + "','LIKE','2026-10-16T10:00:00Z'),"
                    + "('" + post + "','" + learner + "','LOVE','2026-10-16T10:01:00Z')");
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            try (var current = statement.executeQuery("SELECT reaction_type FROM post_reactions")) {
                assertTrue(current.next());
                assertEquals("LOVE", current.getString(1));
                assertFalse(current.next());
            }
            try (var archived = statement.executeQuery("SELECT reaction_type FROM post_reaction_archive")) {
                assertTrue(archived.next());
                assertEquals("LIKE", archived.getString(1));
                assertFalse(archived.next());
            }
        }
    }
}
