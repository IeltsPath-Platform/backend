package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class TopicSequenceCourseMetadataIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static JdbcLearningContentReader reader;
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("86").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void sequenceIncludesOptionalCourseCoverAndTopicDescriptions() {
        jdbc.getJdbcTemplate().update("UPDATE courses SET cover_image_url = ?, description = ? WHERE code = ?",
                "https://cdn.example/course.jpg", "Course overview", "IELTS_6_5");
        jdbc.getJdbcTemplate().update("UPDATE topics SET description = ? WHERE code = ?",
                "Build inference skills", "READING_6_5_INFERENCE");

        var topic = reader.topicSequence(3).stream()
                .filter(item -> item.code().equals("READING_6_5_INFERENCE"))
                .findFirst().orElseThrow();

        assertThat(topic.description()).isEqualTo("Build inference skills");
        assertThat(topic.course().coverImageUrl()).isEqualTo("https://cdn.example/course.jpg");
        assertThat(topic.course().description()).isEqualTo("Course overview");
    }
}
