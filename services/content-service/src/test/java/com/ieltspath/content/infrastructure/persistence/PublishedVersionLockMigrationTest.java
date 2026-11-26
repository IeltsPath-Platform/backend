package com.ieltspath.content.infrastructure.persistence;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class PublishedVersionLockMigrationTest {
    private static final String LOCK_MESSAGE = "published_version_content_locked";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() throws Exception {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE question_versions (
                    id UUID PRIMARY KEY, status TEXT NOT NULL, stem TEXT, options JSONB, answer_spec JSONB,
                    schema_version INT, explanation TEXT, difficulty TEXT, hint TEXT, guide TEXT, guide_steps JSONB,
                    published_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
                )
                """);
        jdbc.execute("""
                CREATE TABLE content_package_versions (
                    id UUID PRIMARY KEY, status TEXT NOT NULL, rules JSONB, schema_version INT, version_number INT,
                    published_at TIMESTAMPTZ, updated_at TIMESTAMPTZ
                )
                """);
        var migration = new ClassPathResource("db/migration/V154__lock_published_versions.sql");
        try (var input = migration.getInputStream()) {
            jdbc.execute(StreamUtils.copyToString(input, StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void cleanTestRows() {
        jdbc.update("DELETE FROM question_versions");
        jdbc.update("DELETE FROM content_package_versions");
    }

    @Test
    void refusesToChangePublishedQuestionContent() {
        UUID versionId = insertQuestionVersion("PUBLISHED");

        assertThatThrownBy(() -> jdbc.update("UPDATE question_versions SET stem='Changed' WHERE id=?", versionId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(LOCK_MESSAGE);

        assertThat(jdbc.queryForObject("SELECT stem FROM question_versions WHERE id=?", String.class, versionId))
                .isEqualTo("Original stem");
    }

    @Test
    void refusesToChangePublishedPackageRules() {
        UUID versionId = insertPackageVersion("PUBLISHED");

        assertThatThrownBy(() -> jdbc.update("UPDATE content_package_versions SET rules=CAST(? AS jsonb) WHERE id=?",
                        "{\"changed\":true}", versionId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(LOCK_MESSAGE);

        assertThat(jdbc.queryForObject("SELECT rules::text FROM content_package_versions WHERE id=?", String.class, versionId))
                .isEqualTo("{}");
    }

    @Test
    void allowsPublishingDraftAndArchivingPublishedQuestion() {
        UUID versionId = insertQuestionVersion("DRAFT");

        jdbc.update("UPDATE question_versions SET status='PUBLISHED', published_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                versionId);
        jdbc.update("UPDATE question_versions SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP WHERE id=?", versionId);

        assertThat(status("question_versions", versionId)).isEqualTo("ARCHIVED");
    }

    @Test
    void allowsPublishingDraftAndArchivingPublishedPackage() {
        UUID versionId = insertPackageVersion("DRAFT");

        jdbc.update("UPDATE content_package_versions SET status='PUBLISHED', published_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                versionId);
        jdbc.update("UPDATE content_package_versions SET status='ARCHIVED', updated_at=CURRENT_TIMESTAMP WHERE id=?", versionId);

        assertThat(status("content_package_versions", versionId)).isEqualTo("ARCHIVED");
    }

    private UUID insertQuestionVersion(String status) {
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,status,stem,options,answer_spec,schema_version,explanation,difficulty,hint,guide,guide_steps) "
                        + "VALUES (? ,?, 'Original stem', '[]'::jsonb, '{}'::jsonb, 1, NULL, NULL, NULL, NULL, NULL)",
                versionId, status);
        return versionId;
    }

    private UUID insertPackageVersion(String status) {
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id,status,rules,schema_version,version_number) "
                        + "VALUES (?,?,'{}'::jsonb,1,1)", versionId, status);
        return versionId;
    }

    private String status(String table, UUID versionId) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, versionId);
    }
}
