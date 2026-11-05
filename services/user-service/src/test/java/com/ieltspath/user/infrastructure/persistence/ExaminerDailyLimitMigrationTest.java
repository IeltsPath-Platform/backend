package com.ieltspath.user.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** An examiner takes 30 new jobs a day, Writing and Speaking together; the old default of 10 moves, a chosen limit stays. */
@Testcontainers(disabledWithoutDocker = true)
class ExaminerDailyLimitMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void examinersOnTheOldDefaultMoveToThirtyAndChosenLimitsStay() {
        String schema = "examiner_limit_migration_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = POSTGRES.getJdbcUrl();
        String url = jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        var dataSource = new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas(schema).defaultSchema(schema).target(MigrationVersion.fromVersion("7"))
                .load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        UUID onDefault = examiner(jdbc, null);
        UUID chosen = examiner(jdbc, 4);
        assertEquals(10, limit(jdbc, onDefault));

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration")
                .schemas(schema).defaultSchema(schema).load().migrate();

        assertEquals(30, limit(jdbc, onDefault));
        assertEquals(4, limit(jdbc, chosen));
        UUID created = examiner(jdbc, null);
        assertEquals(30, limit(jdbc, created));
    }

    private UUID examiner(JdbcTemplate jdbc, Integer dailyLimit) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,email,full_name,password_hash,status) VALUES (?,?,'Examiner','test-only','ACTIVE')",
                id, id + "@example.test");
        if (dailyLimit == null) jdbc.update("INSERT INTO examiner_profiles(user_id) VALUES (?)", id);
        else jdbc.update("INSERT INTO examiner_profiles(user_id, daily_limit) VALUES (?, ?)", id, dailyLimit);
        return id;
    }

    private int limit(JdbcTemplate jdbc, UUID id) {
        return jdbc.queryForObject("SELECT daily_limit FROM examiner_profiles WHERE user_id = ?", Integer.class, id);
    }
}
