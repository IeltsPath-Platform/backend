package com.group01.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class CourseMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("19").load().migrate();
    }

    @Test
    void coursesHaveUniqueCodesAndHalfBandsAndDefaultActiveStatus() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'LOW','Low',5.5)");
            sql.executeUpdate("INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'HIGH','High',6.5)");
            try (var rows = sql.executeQuery("SELECT band_level,status FROM courses WHERE code IN ('LOW','HIGH') ORDER BY band_level")) {
                assertTrue(rows.next());
                assertEquals("5.5", rows.getString(1));
                assertEquals("ACTIVE", rows.getString(2));
                assertTrue(rows.next());
                assertEquals("6.5", rows.getString(1));
                assertFalse(rows.next());
            }
            assertEquals("23505", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    "INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'DUP_BAND','Duplicate',5.5)")).getSQLState());
            assertEquals("23505", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    "INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'LOW','Duplicate',7.5)")).getSQLState());
            for (String band : new String[]{"5.25", "9.5", "-0.5"}) {
                assertEquals("23514", assertThrows(SQLException.class, () -> sql.executeUpdate(
                        "INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'INVALID','Invalid'," + band + ")")).getSQLState());
            }
            assertEquals("23514", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    "INSERT INTO courses (id,code,name,band_level,status) VALUES (gen_random_uuid(),'INVALID','Invalid',7.5,'UNKNOWN')")).getSQLState());
        }
    }

    @Test
    void topicCourseMembershipIsOptionalButMustReferenceAnExistingCourse() throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO topics (id,code,name) VALUES (gen_random_uuid(),'WITHOUT_COURSE','No course')");
            assertEquals("23503", assertThrows(SQLException.class, () -> sql.executeUpdate(
                    "UPDATE topics SET course_id=gen_random_uuid() WHERE code='WITHOUT_COURSE'")).getSQLState());
            sql.executeUpdate("INSERT INTO courses (id,code,name,band_level) VALUES (gen_random_uuid(),'MEMBERSHIP','Membership',4.5)");
            assertEquals(1, sql.executeUpdate("UPDATE topics SET course_id=(SELECT id FROM courses WHERE code='MEMBERSHIP') WHERE code='WITHOUT_COURSE'"));
            try (var rows = sql.executeQuery("SELECT indexname FROM pg_indexes WHERE tablename='topics' AND indexdef LIKE '%(course_id)%'")) {
                assertTrue(rows.next());
            }
        }
    }
}
