package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.JdbcQuestionVersionUsageLookup;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionVersionUsageLookupTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;
    private static JdbcQuestionVersionUsageLookup lookup;

    @BeforeAll
    static void migrate() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        lookup = new JdbcQuestionVersionUsageLookup(jdbc);
    }

    @Test
    void identifiesQuestionVersionUsedByPublishedLesson() {
        UUID versionId = jdbc.queryForObject("""
                SELECT bq.question_version_id FROM lesson_block_questions bq
                JOIN lesson_blocks b ON b.id = bq.block_id
                JOIN lessons l ON l.id = b.lesson_id
                WHERE l.status = 'PUBLISHED' LIMIT 1
                """, UUID.class);

        assertThat(lookup.isUsedByPublishedContent(versionId)).isTrue();
    }

    @Test
    void ignoresQuestionVersionNotUsedByPublishedContent() {
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO questions (id, question_type, skill, status, purpose, created_at, updated_at)
                VALUES (?, 'MULTIPLE_CHOICE', 'READING', 'DRAFT', 'LEARNING', now(), now())
                """, questionId);
        jdbc.update("""
                INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec,
                    schema_version, status, created_at, updated_at)
                VALUES (?, ?, 1, 'fixture', '[]'::jsonb, '{}'::jsonb, 1, 'DRAFT', now(), now())
                """, versionId, questionId);

        assertThat(lookup.isUsedByPublishedContent(versionId)).isFalse();
    }
}
