package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PracticeCoverPhotosMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void photoCoversAreObjectKeysNamedAfterTheirAsset() {
        List<Map<String, Object>> photos = jdbc.queryForList("SELECT id::text AS id, media_reference FROM content_assets "
                + "WHERE asset_type = 'IMAGE' AND media_reference LIKE 'practice/covers/%'", Collections.emptyMap());

        assertThat(photos).hasSizeGreaterThan(50);
        assertThat(photos).allSatisfy(photo ->
                assertThat(photo.get("media_reference")).isEqualTo("practice/covers/" + photo.get("id") + ".webp"));
    }

    @Test
    void everySectionCoverIsAPhotoKeyOrAnIllustration() {
        List<String> covers = jdbc.queryForList("SELECT a.media_reference FROM content_sections s JOIN content_assets a "
                + "ON a.id = s.cover_asset_id", Collections.emptyMap(), String.class);

        assertThat(covers).isNotEmpty();
        assertThat(covers).allSatisfy(reference ->
                assertThat(reference).matches("data:image/svg\\+xml;base64,.+|practice/covers/[0-9a-f-]+\\.webp"));
    }
}
