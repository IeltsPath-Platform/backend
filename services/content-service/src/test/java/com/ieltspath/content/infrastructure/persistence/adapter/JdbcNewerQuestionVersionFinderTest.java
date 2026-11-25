package com.ieltspath.content.infrastructure.persistence.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The newest published version after a given one, ignoring drafts, archived versions and other questions. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcNewerQuestionVersionFinderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;
    private static JdbcNewerQuestionVersionFinder finder;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        finder = new JdbcNewerQuestionVersionFinder(jdbc);
    }

    @Test
    void findsTheHighestPublishedVersionAfterTheGivenOne() {
        UUID question = question();
        UUID v1 = version(question, 1, "PUBLISHED");
        UUID v2 = version(question, 2, "PUBLISHED");
        UUID v3 = version(question, 3, "PUBLISHED");
        version(question, 4, "DRAFT");
        version(question(), 9, "PUBLISHED");

        assertThat(finder.newerPublishedVersion(v1)).contains(v3);
        assertThat(finder.newerPublishedVersion(v2)).contains(v3);
        assertThat(finder.newerPublishedVersion(v3)).isEmpty();
    }

    @Test
    void anArchivedOrDraftOnlyFollowUpIsNotNewer() {
        UUID question = question();
        UUID v1 = version(question, 1, "PUBLISHED");
        version(question, 2, "ARCHIVED");
        version(question, 3, "DRAFT");

        assertThat(finder.newerPublishedVersion(v1)).isEmpty();
    }

    private static UUID question() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED')", id, id);
        return id;
    }

    private static UUID version(UUID question, int number, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES(?, ?, ?, 'Stem', ?)",
                id, question, number, status);
        return id;
    }
}
