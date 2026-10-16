package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The Listening practice tests carry their recordings in the database, so every attempt can play them. */
@Testcontainers(disabledWithoutDocker = true)
class ListeningPracticeAudioSeedTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate sql;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        sql = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void everyListeningPracticeSectionHasAPlayableRecordingStoredInTheDatabase() {
        MediaReferencePolicy noBase = new MediaReferencePolicy("");
        List<Map<String, Object>> sections = sql.queryForList("""
                SELECT s.id, a.media_reference, a.duration_seconds, a.text_content
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id AND s.skill = 'LISTENING'
                LEFT JOIN content_asset_links l ON l.section_id = s.id
                LEFT JOIN content_assets a ON a.id = l.asset_id AND a.asset_type = 'AUDIO'
                WHERE p.package_type = 'PRACTICE_TEST' AND p.code LIKE 'PT-LISTENING-%'
                """);

        assertThat(sections).hasSize(12).allSatisfy(section -> {
            String reference = (String) section.get("media_reference");
            assertThat(reference).startsWith("data:audio/mpeg;base64,");
            assertThat(noBase.resolve(AssetType.AUDIO, reference)).isEqualTo(reference);
            assertThat((Integer) section.get("duration_seconds")).isBetween(20, 120);
            assertThat((String) section.get("text_content")).isNotBlank();
        });
    }
}
