package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcContentOverviewReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class JdbcContentOverviewReaderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcTemplate jdbc;
    private static JdbcContentOverviewReader reader;

    @BeforeAll
    static void migrate() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        reader = new JdbcContentOverviewReader(jdbc);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    @Test
    void knowledgePointCountsMatchTheTable() {
        ContentOverviewResult overview = reader.read();

        assertThat(overview.knowledgePoints().total()).isEqualTo(count("SELECT count(*) FROM knowledge_points"));
        assertThat(overview.knowledgePoints().byStatus().get("ACTIVE"))
                .isEqualTo(count("SELECT count(*) FROM knowledge_points WHERE status = 'ACTIVE'"));
        assertThat(overview.knowledgePoints().bySkill().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(count("SELECT count(*) FROM knowledge_points WHERE status = 'ACTIVE'"));
        assertThat(overview.knowledgePoints().byCefr().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(count("SELECT count(*) FROM knowledge_points WHERE status = 'ACTIVE'"));
    }

    @Test
    void lessonCountsListEveryStatusEvenWhenEmpty() {
        ContentOverviewResult overview = reader.read();

        assertThat(overview.lessons()).containsKeys("DRAFT", "PUBLISHED", "ARCHIVED");
        assertThat(overview.lessons().get("PUBLISHED"))
                .isEqualTo(count("SELECT count(*) FROM lessons WHERE status = 'PUBLISHED'"));
    }

    @Test
    void countsActivePointsWithoutAPublishedQuestionAndDraftVersions() {
        ContentOverviewResult overview = reader.read();

        assertThat(overview.activeKnowledgePointsWithoutPublishedQuestion()).isEqualTo(count("""
                SELECT count(*) FROM knowledge_points kp WHERE kp.status = 'ACTIVE' AND kp.id NOT IN (
                    SELECT qkp.knowledge_point_id FROM question_knowledge_points qkp
                    JOIN question_versions qv ON qv.id = qkp.question_version_id WHERE qv.status = 'PUBLISHED')
                """));
        assertThat(overview.draftQuestionVersions())
                .isEqualTo(count("SELECT count(*) FROM question_versions WHERE status = 'DRAFT'"));
    }
}
