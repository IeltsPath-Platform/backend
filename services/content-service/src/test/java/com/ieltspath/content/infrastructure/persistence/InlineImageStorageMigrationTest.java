package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class InlineImageStorageMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate sql;
    private static int imageRowsBeforeMigration;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("89").load().migrate();
        sql = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        imageRowsBeforeMigration = sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE'", Integer.class);
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE media_reference LIKE 'data:image/svg+xml;base64,%'", Integer.class))
                .isEqualTo(86);
        // Stops at the storage migrations: later Reading seeds add covers that stay inline illustrations until a photo is chosen.
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("93").load().migrate();
    }

    @Test
    void movesAllInlineImagesToTheirStorageKeysWithoutChangingImageRows() {
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE media_reference LIKE 'data:%'", Integer.class))
                .isZero();
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE'", Integer.class))
                .isEqualTo(imageRowsBeforeMigration);
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE' AND (media_reference LIKE 'practice/covers/%' OR media_reference LIKE 'writing/charts/%')", Integer.class))
                .isEqualTo(86);
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE' AND media_reference LIKE 'practice/covers/%'", Integer.class))
                .isEqualTo(70);
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE' AND media_reference LIKE 'writing/charts/%'", Integer.class))
                .isEqualTo(16);
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = 'IMAGE' AND (media_reference LIKE 'practice/covers/%' OR media_reference LIKE 'writing/charts/%') AND media_reference NOT IN ('practice/covers/' || id || '.webp', 'writing/charts/' || id || '.webp')", Integer.class))
                .isZero();
    }
}
