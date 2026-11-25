package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A part number is unique among the sections of one skill in a version, not across the whole version. */
@Testcontainers(disabledWithoutDocker = true)
class SectionPartNumberUniquenessTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void differentSkillsCanUseTheSamePartNumberButOneSkillCannotRepeatIt() {
        UUID version = version();
        section(version, 1, "LISTENING", 1);
        section(version, 2, "READING", 1);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_sections WHERE package_version_id = ? AND part_no = 1",
                Integer.class, version)).isEqualTo(2);
        assertThatThrownBy(() -> section(version, 3, "READING", 1)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void sectionsWithoutAPartNumberAreNotConstrained() {
        UUID version = version();
        section(version, 1, "READING", null);
        section(version, 2, "READING", null);
    }

    private static UUID version() {
        UUID packageId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type) VALUES(?,?,'Fixture','MOCK_TEST')", packageId, "PART_P_" + packageId);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number) VALUES(?,?,1)", version, packageId);
        return version;
    }

    private static void section(UUID version, int order, String skill, Integer partNo) {
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order,part_no) VALUES(?,?,?,?,?,?)",
                UUID.randomUUID(), version, "Section " + order, skill, order, partNo);
    }
}
