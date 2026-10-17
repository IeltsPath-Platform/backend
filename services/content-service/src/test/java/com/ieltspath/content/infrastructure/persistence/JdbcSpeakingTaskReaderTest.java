package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.port.SpeakingTaskReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcSpeakingTaskReader;
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

/** The seeded Speaking practice tests as the recording workspace reads them: one part, its questions in order. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcSpeakingTaskReaderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DataSource dataSource;
    private Connection connection;
    private JdbcTemplate sql;
    private JdbcSpeakingTaskReader reader;

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
        reader = new JdbcSpeakingTaskReader(sql);
    }

    @AfterEach
    void rollback() throws Exception {
        connection.rollback();
        connection.close();
    }

    private UUID version(String code) {
        return sql.queryForObject("SELECT current_published_version_id FROM content_packages WHERE code = ?",
                UUID.class, code);
    }

    private SpeakingTaskReader.SpeakingTask part(String code, int partNo) {
        UUID version = version(code);
        UUID section = sql.queryForObject("SELECT id FROM content_sections WHERE package_version_id = ? AND part_no = ?",
                UUID.class, version, partNo);
        return reader.find(version, section).orElseThrow();
    }

    @Test
    void partOneListsItsFourQuestionsInOrder() {
        var task = part("PT-SPEAKING-01", 1);

        assertThat(task.title()).isEqualTo("Part 1: Hometown");
        assertThat(task.partNo()).isEqualTo(1);
        assertThat(task.timeLimitSeconds()).isEqualTo(300);
        assertThat(task.instructions()).contains("general questions");
        assertThat(task.questions()).hasSize(4)
                .extracting(SpeakingTaskReader.Question::sortOrder).isSorted();
        assertThat(task.questions()).allSatisfy(question -> {
            assertThat(question.stem()).isNotBlank();
            assertThat(question.durationSeconds()).isPositive();
        });
    }

    @Test
    void partTwoIsTheCueCardOfTheTest() {
        var task = part("PT-SPEAKING-01", 2);

        assertThat(task.questions()).singleElement().satisfies(question ->
                assertThat(question.stem()).startsWith("Describe").contains("You should say"));
    }

    @Test
    void everySeededSpeakingPartHasQuestions() {
        List<String> empty = sql.queryForList("""
                SELECT p.code || ' part ' || s.part_no
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id
                WHERE p.package_type = 'PRACTICE_TEST' AND s.skill = 'SPEAKING'
                  AND NOT EXISTS (SELECT 1 FROM section_questions sq WHERE sq.section_id = s.id)
                """, String.class);
        assertThat(empty).isEmpty();
    }

    @Test
    void aSectionOfAnotherSkillIsNotASpeakingTask() {
        UUID version = version("PT-LISTENING-01");
        UUID section = sql.queryForObject("SELECT id FROM content_sections WHERE package_version_id = ? AND part_no = 1",
                UUID.class, version);

        assertThat(reader.find(version, section)).isEmpty();
    }
}
