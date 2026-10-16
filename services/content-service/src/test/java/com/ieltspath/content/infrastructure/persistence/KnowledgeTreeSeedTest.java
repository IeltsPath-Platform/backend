package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class KnowledgeTreeSeedTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("30").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void seedsOnlyTheInactiveCoursesAndSkillTopicsForBandsSixAndSeven() {
        var courses = jdbc.queryForList("SELECT id,code,band_level,status FROM courses "
                + "WHERE code IN ('IELTS_6_0','IELTS_7_0') ORDER BY band_level", Map.of());
        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(row -> row.get("id")).containsExactly(
                UUID.fromString("30000000-0000-4000-8000-000000000003"),
                UUID.fromString("30000000-0000-4000-8000-000000000004"));
        assertThat(courses).extracting(row -> row.get("band_level")).containsExactly(
                new BigDecimal("6.0"), new BigDecimal("7.0"));
        assertThat(courses).extracting(row -> row.get("status")).containsOnly("INACTIVE");

        var topics = jdbc.queryForList("SELECT t.id,t.code,t.skill,t.status,t.course_id,c.code AS course_code "
                + "FROM topics t JOIN courses c ON c.id=t.course_id "
                + "WHERE c.code IN ('IELTS_6_0','IELTS_7_0') ORDER BY c.band_level,t.sort_order", Map.of());
        assertThat(topics).hasSize(6);
        assertThat(topics).extracting(row -> row.get("id")).containsExactly(
                UUID.fromString("31000000-0000-4000-8000-000000000001"),
                UUID.fromString("31000000-0000-4000-8000-000000000002"),
                UUID.fromString("31000000-0000-4000-8000-000000000003"),
                UUID.fromString("31000000-0000-4000-8000-000000000004"),
                UUID.fromString("31000000-0000-4000-8000-000000000005"),
                UUID.fromString("31000000-0000-4000-8000-000000000006"));
        assertThat(topics).extracting(row -> row.get("code")).containsExactly(
                "IELTS_6_0_READING", "IELTS_6_0_LISTENING", "IELTS_6_0_WRITING",
                "IELTS_7_0_READING", "IELTS_7_0_LISTENING", "IELTS_7_0_WRITING");
        assertThat(topics).extracting(row -> row.get("skill")).containsExactly(
                "READING", "LISTENING", "WRITING", "READING", "LISTENING", "WRITING");
        assertThat(topics).extracting(row -> row.get("status")).containsOnly("INACTIVE");
        assertThat(topics).extracting(row -> row.get("course_code")).containsExactly(
                "IELTS_6_0", "IELTS_6_0", "IELTS_6_0", "IELTS_7_0", "IELTS_7_0", "IELTS_7_0");
        assertThat(topics).extracting(row -> row.get("course_id")).containsExactly(
                UUID.fromString("30000000-0000-4000-8000-000000000003"),
                UUID.fromString("30000000-0000-4000-8000-000000000003"),
                UUID.fromString("30000000-0000-4000-8000-000000000003"),
                UUID.fromString("30000000-0000-4000-8000-000000000004"),
                UUID.fromString("30000000-0000-4000-8000-000000000004"),
                UUID.fromString("30000000-0000-4000-8000-000000000004"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM knowledge_points kp JOIN topics t ON t.id=kp.topic_id "
                + "WHERE t.id::text LIKE '31000000-%'", Map.of(), Integer.class)).isZero();
    }
}
