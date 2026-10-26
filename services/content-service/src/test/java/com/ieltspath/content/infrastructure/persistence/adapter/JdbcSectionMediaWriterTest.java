package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.result.AuthorPackageExtras;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
class JdbcSectionMediaWriterTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcSectionMediaWriter writer;
    private static JdbcAuthorPackageReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        writer = new JdbcSectionMediaWriter(jdbc);
        reader = new JdbcAuthorPackageReader(jdbc);
    }

    @Test
    void settingThePassageTwiceLeavesOneLinkAndKeepsTheOldAsset() {
        Section s = section();

        writer.setPassage(s.id(), "first");
        writer.setPassage(s.id(), "second");

        assertEquals("second", reader.extras(s.packageId()).media(s.id()).passage());
        assertEquals(1, links(s.id(), "PASSAGE"));
        assertEquals(2, assets("PASSAGE", "first", "second"));
    }

    @Test
    void clearingThePassageUnlinksItOnly() {
        Section s = section();
        writer.setPassage(s.id(), "text");
        writer.setAudio(s.id(), "listening/demo/ls1.mp3", 30, null);

        writer.clearPassage(s.id());

        assertNull(reader.extras(s.packageId()).media(s.id()).passage());
        assertNotNull(reader.extras(s.packageId()).media(s.id()).audio());
        assertEquals(0, links(s.id(), "PASSAGE"));
    }

    @Test
    void settingTheAudioReplacesTheOldOneAndKeepsThePassage() {
        Section s = section();
        writer.setPassage(s.id(), "kept");
        UUID first = writer.setAudio(s.id(), "listening/demo/one.mp3", 10, "one");
        UUID second = writer.setAudio(s.id(), "listening/demo/two.mp3", 20, "two");

        AuthorPackageExtras.SectionMedia media = reader.extras(s.packageId()).media(s.id());

        assertNotEquals(first, second);
        assertEquals(second, media.audio().assetId());
        assertEquals("listening/demo/two.mp3", media.audio().mediaReference());
        assertEquals(20, media.audio().durationSeconds());
        assertEquals("two", media.audio().transcript());
        assertEquals("kept", media.passage());
        assertEquals(1, links(s.id(), "AUDIO"));
    }

    @Test
    void clearingTheAudioKeepsTheAssetRow() {
        Section s = section();
        UUID asset = writer.setAudio(s.id(), "listening/demo/one.mp3", 10, null);

        writer.clearAudio(s.id());

        assertNull(reader.extras(s.packageId()).media(s.id()).audio());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM content_assets WHERE id = :id",
                Map.of("id", asset), Integer.class));
    }

    @Test
    void anotherSectionIsUntouched() {
        Section a = section();
        Section b = section();
        writer.setPassage(a.id(), "A");
        writer.setPassage(b.id(), "B");

        writer.setPassage(a.id(), "A2");
        writer.clearPassage(a.id());

        assertEquals("B", reader.extras(b.packageId()).media(b.id()).passage());
    }

    private record Section(UUID packageId, UUID id) {
    }

    private static Section section() {
        UUID packageId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages (id, code, title, package_type) "
                        + "VALUES (:id, :code, 'T', 'PRACTICE_SET')",
                Map.of("id", packageId, "code", "MW" + packageId.toString().replace("-", "").substring(0, 16)));
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions (id, package_id, version_number) VALUES (:id, :pkg, 1)",
                Map.of("id", versionId, "pkg", packageId));
        UUID sectionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections (id, package_version_id, title, sort_order) "
                        + "VALUES (:id, :version, 'S', 1)", Map.of("id", sectionId, "version", versionId));
        return new Section(packageId, sectionId);
    }

    private static int links(UUID sectionId, String type) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM content_asset_links l JOIN content_assets a ON a.id = l.asset_id
                WHERE l.section_id = :section AND a.asset_type = :type
                """, new MapSqlParameterSource("section", sectionId).addValue("type", type), Integer.class);
    }

    private static int assets(String type, String... texts) {
        return jdbc.queryForObject("SELECT count(*) FROM content_assets WHERE asset_type = :type "
                        + "AND text_content IN (:texts)",
                new MapSqlParameterSource("type", type).addValue("texts", java.util.List.of(texts)), Integer.class);
    }
}
