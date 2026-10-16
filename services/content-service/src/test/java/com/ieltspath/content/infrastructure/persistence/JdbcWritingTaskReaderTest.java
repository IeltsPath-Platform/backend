package com.ieltspath.content.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.WritingTaskReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcWritingTaskReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The seeded Writing practice tests as the workspace reads them: prompt, chart, guide and six steps. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcWritingTaskReaderTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DataSource dataSource;
    private Connection connection;
    private JdbcTemplate sql;
    private JdbcWritingTaskReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source = new org.postgresql.ds.PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        dataSource = source;
    }

    @BeforeEach
    void begin() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        sql = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        reader = new JdbcWritingTaskReader(sql);
    }

    @AfterEach
    void rollback() throws Exception {
        connection.rollback();
        connection.close();
    }

    private WritingTaskReader.WritingTask task(String code, int partNo) {
        UUID version = sql.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = ?",
                UUID.class, code);
        UUID section = sql.queryForObject("SELECT id FROM content_sections WHERE package_version_id = ? AND part_no = ?",
                UUID.class, version, partNo);
        return reader.find(version, section).orElseThrow();
    }

    @Test
    void taskOneCarriesItsChartGuideAndSixSteps() throws Exception {
        var task = task("PT-WRITING-01", 1);

        assertThat(task.task()).isEqualTo("TASK_1");
        assertThat(task.minWords()).isEqualTo(150);
        assertThat(task.timeLimitSeconds()).isEqualTo(1200);
        assertThat(task.stem()).contains("line graph");
        assertThat(task.images()).singleElement().satisfies(image -> {
            assertThat(image.mediaReference()).startsWith("data:image/svg+xml;base64,");
            assertThat(image.altText()).contains("Line graph");
        });
        assertThat(task.guide()).contains("## 1. Giải thích đề");
        JsonNode steps = JSON.readTree(task.guideSteps());
        assertThat(steps).hasSize(6);
        assertThat(steps.get(0).get("section").asText()).isEqualTo("Introduction");
        assertThat(steps.get(5).get("vocabulary").get(0).get("vi").asText()).isNotBlank();
    }

    @Test
    void everyWritingPromptHasAGuideAndSixSteps() {
        List<String> missing = sql.queryForList("""
                SELECT p.code || ' part ' || s.part_no
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE p.package_type = 'PRACTICE_TEST' AND s.skill = 'WRITING'
                  AND (qv.guide IS NULL OR jsonb_array_length(COALESCE(qv.guide_steps, '[]'::jsonb)) <> 6)
                """, String.class);
        assertThat(missing).isEmpty();
    }

    @Test
    void everyTaskOneHasAChartAndTaskTwoHasNone() {
        List<String> wrong = sql.queryForList("""
                SELECT p.code || ' part ' || s.part_no
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                JOIN section_questions sq ON sq.section_id = s.id
                WHERE p.package_type = 'PRACTICE_TEST' AND s.skill = 'WRITING'
                  AND (SELECT count(*) FROM content_asset_links l WHERE l.question_version_id = sq.question_version_id)
                      <> CASE WHEN s.part_no = 1 THEN 1 ELSE 0 END
                """, String.class);
        assertThat(wrong).isEmpty();
    }

    @Test
    void aSectionOfAnotherSkillIsNotAWritingTask() {
        UUID version = sql.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = ?",
                UUID.class, "PT-LISTENING-01");
        UUID section = sql.queryForObject("SELECT id FROM content_sections WHERE package_version_id = ? AND part_no = 1",
                UUID.class, version);

        assertThat(reader.find(version, section)).isEmpty();
    }
}
