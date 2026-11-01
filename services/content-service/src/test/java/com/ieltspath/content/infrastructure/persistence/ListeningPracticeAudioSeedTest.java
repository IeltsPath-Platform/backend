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

/** The Listening practice tests reference their recordings by object key; the audio itself is never in the database. */
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
    void everyListeningPracticeSectionPointsAtAnObjectKeyUnderTheMediaBase() {
        MediaReferencePolicy policy = new MediaReferencePolicy("https://media.example.test/ieltspath");
        List<Map<String, Object>> sections = sql.queryForList("""
                SELECT s.id, a.media_reference, a.duration_seconds, a.text_content
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id
                JOIN content_sections s ON s.package_version_id = v.id AND s.skill = 'LISTENING'
                LEFT JOIN content_asset_links l ON l.section_id = s.id
                LEFT JOIN content_assets a ON a.id = l.asset_id AND a.asset_type = 'AUDIO'
                WHERE p.package_type = 'PRACTICE_TEST' AND p.code LIKE 'PT-LISTENING-%'
                """);

        // Tests 01-03 have short recordings (under two minutes); tests 04 and later are full-length, with recordings of several minutes.
        assertThat(sections).hasSize(32).allSatisfy(section -> {
            String reference = (String) section.get("media_reference");
            assertThat(reference).startsWith("listening/practice/").endsWith(".mp3");
            assertThat(policy.resolve(AssetType.AUDIO, reference))
                    .isEqualTo("https://media.example.test/ieltspath/" + reference);
            assertThat((Integer) section.get("duration_seconds")).isBetween(20, 600);
            assertThat((String) section.get("text_content")).isNotBlank();
        });
    }

    @Test
    void noAudioAssetHoldsADataUri() {
        assertThat(sql.queryForObject(
                "SELECT count(*) FROM content_assets WHERE asset_type = 'AUDIO' AND media_reference LIKE 'data:%'",
                Integer.class)).isZero();
    }
}
