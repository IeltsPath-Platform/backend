package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.vo.RubricVersionRef;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcRubricVersionsTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void everyVersionOfACodeIsListedIncludingRetired() {
        String code = "RUBRIC-" + UUID.randomUUID();
        insert(code, 1, "RETIRED");
        insert(code, 2, "RETIRED");
        insert(code, 3, "DRAFT");
        insert(code + "-OTHER", 1, "DRAFT");

        List<RubricVersionRef> versions = new JdbcRubricRepository(jdbc).findVersions(code);

        assertEquals(List.of(
                new RubricVersionRef(code, "TASK_2", 1, "RETIRED"),
                new RubricVersionRef(code, "TASK_2", 2, "RETIRED"),
                new RubricVersionRef(code, "TASK_2", 3, "DRAFT")), versions);
    }

    @Test
    void unknownCodeHasNoVersions() {
        assertTrue(new JdbcRubricRepository(jdbc).findVersions("NO-SUCH-" + UUID.randomUUID()).isEmpty());
    }

    private static void insert(String code, int version, String status) {
        jdbc.update("INSERT INTO rubrics (code, task, rubric_version, status, source_note) "
                + "VALUES (:code, 'TASK_2', :version, :status, 'test')",
                java.util.Map.of("code", code, "version", version, "status", status));
    }
}
