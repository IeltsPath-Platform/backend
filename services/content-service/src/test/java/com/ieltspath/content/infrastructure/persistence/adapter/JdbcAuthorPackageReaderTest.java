package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JdbcAuthorPackageReaderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;
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
        reader = new JdbcAuthorPackageReader(jdbc);
    }

    @Test
    void listReportsLatestVersionAndWhetherADraftExists() {
        String tag = tag();
        UUID withDraft = insertPackage(tag + "-A", "PRACTICE_SET", "PUBLISHED");
        insertVersion(withDraft, 1, "PUBLISHED");
        insertVersion(withDraft, 2, "DRAFT");
        UUID withoutDraft = insertPackage(tag + "-B", "PRACTICE_SET", "PUBLISHED");
        insertVersion(withoutDraft, 1, "PUBLISHED");
        UUID empty = insertPackage(tag + "-C", "QUIZ", "DRAFT");

        List<AuthorPackageSummary> rows = reader.list(filter(null, null, tag));

        assertEquals(3, rows.size());
        AuthorPackageSummary a = byId(rows, withDraft);
        assertEquals(2, a.latestVersionNumber());
        assertTrue(a.hasDraft());
        AuthorPackageSummary b = byId(rows, withoutDraft);
        assertEquals(1, b.latestVersionNumber());
        assertFalse(b.hasDraft());
        AuthorPackageSummary c = byId(rows, empty);
        assertNull(c.latestVersionNumber());
        assertFalse(c.hasDraft());
    }

    @Test
    void listFiltersByTypeStatusAndText() {
        String tag = tag();
        insertPackage(tag + "-SET", "PRACTICE_SET", "DRAFT");
        insertPackage(tag + "-QUIZ", "QUIZ", "PUBLISHED");

        assertEquals(1, reader.list(filter(PackageType.QUIZ, null, tag)).size());
        assertEquals(1, reader.list(filter(null, PublicationStatus.DRAFT, tag)).size());
        assertEquals(0, reader.list(filter(PackageType.QUIZ, PublicationStatus.DRAFT, tag)).size());
        assertEquals(1, reader.list(filter(null, null, tag.toLowerCase() + "-quiz")).size());
    }

    @Test
    void searchTextIsLiteralNotAPattern() {
        String tag = tag();
        insertPackage(tag + "-X", "QUIZ", "DRAFT");

        assertEquals(0, reader.list(filter(null, null, "%")).stream()
                .filter(row -> row.code().startsWith(tag)).count());
        assertEquals(0, reader.list(filter(null, null, tag + "_X")).size());
    }

    @Test
    void listFiltersByTopicAndLessonAndCourse() {
        UUID topic = UUID.fromString("23000000-0000-4000-8000-010000000001");
        List<AuthorPackageSummary> finals = reader.list(new AuthorPackageReader.Filter(
                PackageType.TOPIC_TEST, null, null, topic, null, null));

        assertFalse(finals.isEmpty());
        assertTrue(finals.stream().allMatch(row -> topic.equals(row.topicId())));
    }

    @Test
    void extrasCarryPassageAudioAndQuestionDetails() {
        String tag = tag();
        UUID pkg = insertPackage(tag, "PRACTICE_SET", "DRAFT");
        UUID version = insertVersion(pkg, 1, "DRAFT");
        UUID section = insertSection(version, 1);
        UUID question = insertQuestion("LEARNING", "PUBLISHED", "x".repeat(300));
        UUID sectionQuestion = insertSectionQuestion(section, question, 1);
        linkAsset(section, "PASSAGE", "The passage text", null, null, 0);
        linkAsset(section, "AUDIO", "A transcript", "listening/demo/ls1.mp3", 45, 0);
        UUID bare = insertSection(version, 2);

        AuthorPackageExtras extras = reader.extras(pkg);

        AuthorPackageExtras.SectionMedia media = extras.media(section);
        assertEquals("The passage text", media.passage());
        assertNotNull(media.audio());
        assertEquals("listening/demo/ls1.mp3", media.audio().mediaReference());
        assertEquals(45, media.audio().durationSeconds());
        assertEquals("A transcript", media.audio().transcript());
        assertNull(extras.media(bare).passage());
        assertNull(extras.media(bare).audio());

        AuthorPackageExtras.QuestionInfo info = extras.question(sectionQuestion);
        assertNotNull(info);
        assertEquals("PUBLISHED", info.status());
        assertEquals("LEARNING", info.purpose());
        assertEquals(200, info.stem().length());
    }

    @Test
    void onlyTheFirstPassageAndAudioAreReported() {
        UUID pkg = insertPackage(tag(), "PRACTICE_SET", "DRAFT");
        UUID section = insertSection(insertVersion(pkg, 1, "DRAFT"), 1);
        linkAsset(section, "PASSAGE", "second", null, null, 5);
        linkAsset(section, "PASSAGE", "first", null, null, 1);

        assertEquals("first", reader.extras(pkg).media(section).passage());
    }

    private static AuthorPackageReader.Filter filter(PackageType type, PublicationStatus status, String q) {
        return new AuthorPackageReader.Filter(type, status, null, null, null, q);
    }

    private static AuthorPackageSummary byId(List<AuthorPackageSummary> rows, UUID id) {
        return rows.stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow();
    }

    private static String tag() {
        return "RDR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private static UUID insertPackage(String code, String type, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages (id, code, title, package_type, status) "
                + "VALUES (:id, :code, :code, :type, :status)",
                Map.of("id", id, "code", code, "type", type, "status", status));
        return id;
    }

    private static UUID insertVersion(UUID packageId, int number, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions (id, package_id, version_number, status) "
                + "VALUES (:id, :pkg, :number, :status)",
                Map.of("id", id, "pkg", packageId, "number", number, "status", status));
        return id;
    }

    private static UUID insertSection(UUID versionId, int order) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections (id, package_version_id, title, skill, sort_order) "
                + "VALUES (:id, :version, 'Section', 'READING', :order)",
                Map.of("id", id, "version", versionId, "order", order));
        return id;
    }

    private static UUID insertQuestion(String purpose, String versionStatus, String stem) {
        UUID questionId = UUID.randomUUID();
        jdbc.update("INSERT INTO questions (id, question_type, skill, purpose, status) "
                + "VALUES (:id, 'MULTIPLE_CHOICE', 'READING', :purpose, 'DRAFT')",
                Map.of("id", questionId, "purpose", purpose));
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions (id, question_id, version_number, stem, status) "
                + "VALUES (:id, :question, 1, :stem, :status)",
                Map.of("id", versionId, "question", questionId, "stem", stem, "status", versionStatus));
        return versionId;
    }

    private static UUID insertSectionQuestion(UUID sectionId, UUID questionVersionId, int order) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO section_questions (id, section_id, question_version_id, sort_order) "
                + "VALUES (:id, :section, :version, :order)",
                Map.of("id", id, "section", sectionId, "version", questionVersionId, "order", order));
        return id;
    }

    private static void linkAsset(UUID sectionId, String type, String text, String reference, Integer duration,
                                  int order) {
        UUID asset = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds) "
                        + "VALUES (:id, :type, :text, :reference, :duration)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("id", asset).addValue("type", type).addValue("text", text)
                        .addValue("reference", reference).addValue("duration", duration));
        jdbc.update("INSERT INTO content_asset_links (asset_id, section_id, sort_order) VALUES (:asset, :section, :order)",
                Map.of("asset", asset, "section", sectionId, "order", order));
    }
}
