package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.AuthorVersionResult;
import com.ieltspath.content.domain.exception.DraftVersionExistsException;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPackageVersionCopier;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A new draft on the real schema: what it copies from the version learners get, and what it leaves alone. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CreateDraftFromCurrentVersionUseCase.class, JdbcPackageVersionCopier.class,
        ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
class CreateDraftFromCurrentVersionIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired CreateDraftFromCurrentVersionUseCase useCase;

    @Test
    void theDraftCopiesSectionsGroupsQuestionLinksAndMediaFromTheCurrentVersion() {
        Fixture f = publishedPackage();

        AuthorVersionResult draft = useCase.execute(f.packageId, null);

        assertThat(draft.versionNumber()).isEqualTo(2);
        assertThat(draft.status().name()).isEqualTo("DRAFT");
        assertThat(draft.rulesJson()).contains("\"timed\"");
        List<Map<String, Object>> sections = jdbc.queryForList(
                "SELECT id, title, skill, sort_order, time_limit_seconds, instructions, part_no FROM content_sections "
                        + "WHERE package_version_id = ? ORDER BY sort_order", draft.id());
        assertThat(sections).extracting(row -> row.get("title")).containsExactly("Listening 1", "Reading 1");
        assertThat(sections.get(0)).containsEntry("skill", "LISTENING").containsEntry("time_limit_seconds", 600)
                .containsEntry("instructions", "Listen.").containsEntry("part_no", 1);
        assertThat(sections).extracting(row -> row.get("id")).doesNotContain(f.listening, f.reading);

        UUID newListening = (UUID) sections.get(0).get("id");
        List<Map<String, Object>> groups = jdbc.queryForList(
                "SELECT id, format_code, instructions, sort_order FROM section_question_groups WHERE section_id = ?", newListening);
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).containsEntry("format_code", "NOTE_COMPLETION").containsEntry("instructions", "Complete the notes.");
        assertThat(groups.get(0).get("id")).isNotEqualTo(f.group);

        List<Map<String, Object>> links = jdbc.queryForList(
                "SELECT question_version_id, sort_order, max_score, group_id FROM section_questions WHERE section_id = ? ORDER BY sort_order",
                newListening);
        assertThat(links).extracting(row -> row.get("question_version_id")).containsExactly(f.firstQuestion, f.secondQuestion);
        assertThat(links.get(0).get("group_id")).isEqualTo(groups.get(0).get("id"));
        assertThat(links.get(1).get("group_id")).isNull();
        assertThat(links.get(1).get("max_score")).hasToString("2.50");

        assertThat(jdbc.queryForList("SELECT asset_id FROM content_asset_links WHERE section_id = ?", UUID.class, newListening))
                .containsExactlyInAnyOrder(f.passageAsset, f.audioAsset);
    }

    @Test
    void editingTheDraftLeavesThePublishedVersionUntouched() {
        Fixture f = publishedPackage();
        AuthorVersionResult draft = useCase.execute(f.packageId, null);

        jdbc.update("UPDATE content_sections SET title = 'Changed' WHERE package_version_id = ?", draft.id());
        jdbc.update("DELETE FROM section_questions WHERE section_id IN (SELECT id FROM content_sections WHERE package_version_id = ?)", draft.id());
        jdbc.update("DELETE FROM content_sections WHERE package_version_id = ? AND sort_order = 2", draft.id());

        assertThat(jdbc.queryForList("SELECT title FROM content_sections WHERE package_version_id = ? ORDER BY sort_order",
                String.class, f.version)).containsExactly("Listening 1", "Reading 1");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM section_questions WHERE section_id = ?", Integer.class, f.listening)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM section_question_groups WHERE section_id = ?", Integer.class, f.listening)).isEqualTo(1);
    }

    @Test
    void anExplicitRulesValueWinsOverTheSourceRules() {
        Fixture f = publishedPackage();

        AuthorVersionResult draft = useCase.execute(f.packageId, "{\"other\": true}");

        assertThat(draft.rulesJson()).contains("other").doesNotContain("timed");
    }

    @Test
    void aPackageWithoutAnEarlierVersionGetsAnEmptyDraft() {
        UUID packageId = UUID.randomUUID();
        insertPackage(packageId);

        AuthorVersionResult draft = useCase.execute(packageId, null);

        assertThat(draft.versionNumber()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_sections WHERE package_version_id = ?", Integer.class, draft.id()))
                .isZero();
    }

    @Test
    void aSecondDraftIsRefusedAndNothingIsCopiedTwice() {
        Fixture f = publishedPackage();
        AuthorVersionResult draft = useCase.execute(f.packageId, null);

        assertThatThrownBy(() -> useCase.execute(f.packageId, null)).isInstanceOf(DraftVersionExistsException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_sections WHERE package_version_id = ?", Integer.class, draft.id()))
                .isEqualTo(2);
    }

    private record Fixture(UUID packageId, UUID version, UUID listening, UUID reading, UUID group, UUID firstQuestion,
                           UUID secondQuestion, UUID passageAsset, UUID audioAsset) {
    }

    private void insertPackage(UUID packageId) {
        jdbc.update("INSERT INTO content_packages(id, code, title, package_type) VALUES(?, ?, 'Placement', 'PRACTICE_SET')",
                packageId, "CD" + packageId.toString().replace("-", "").substring(0, 16));
    }

    private Fixture publishedPackage() {
        UUID packageId = UUID.randomUUID();
        insertPackage(packageId);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id, package_id, version_number, status, rules) "
                + "VALUES(?, ?, 1, 'PUBLISHED', '{\"timed\": true}'::jsonb)", version, packageId);
        jdbc.update("UPDATE content_packages SET status = 'PUBLISHED', current_published_version_id = ? WHERE id = ?", version, packageId);

        UUID listening = UUID.randomUUID();
        UUID reading = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no) "
                + "VALUES(?, ?, 'Listening 1', 'LISTENING', 1, 600, 'Listen.', 1)", listening, version);
        jdbc.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order) "
                + "VALUES(?, ?, 'Reading 1', 'READING', 2)", reading, version);

        UUID group = UUID.randomUUID();
        jdbc.update("INSERT INTO section_question_groups(id, section_id, sort_order, format_code, instructions) "
                + "VALUES(?, ?, 1, 'NOTE_COMPLETION', 'Complete the notes.')", group, listening);
        UUID first = questionVersion();
        UUID second = questionVersion();
        jdbc.update("INSERT INTO section_questions(section_id, question_version_id, sort_order, max_score, group_id) VALUES(?, ?, 1, 1, ?)",
                listening, first, group);
        jdbc.update("INSERT INTO section_questions(section_id, question_version_id, sort_order, max_score) VALUES(?, ?, 2, 2.5)",
                listening, second);

        UUID passage = UUID.randomUUID();
        UUID audio = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets(id, asset_type, text_content) VALUES(?, 'PASSAGE', 'A passage.')", passage);
        jdbc.update("INSERT INTO content_assets(id, asset_type, media_reference, duration_seconds) VALUES(?, 'AUDIO', 'listening/demo/a.mp3', 30)", audio);
        jdbc.update("INSERT INTO content_asset_links(asset_id, section_id, sort_order) VALUES(?, ?, 0)", passage, listening);
        jdbc.update("INSERT INTO content_asset_links(asset_id, section_id, sort_order) VALUES(?, ?, 0)", audio, listening);
        return new Fixture(packageId, version, listening, reading, group, first, second, passage, audio);
    }

    private UUID questionVersion() {
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id, code, question_type, skill, purpose, status) "
                + "VALUES(?, 'Q-' || replace(?::text, '-', ''), 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED')", question, question);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES(?, ?, 1, 'Stem', 'PUBLISHED')",
                version, question);
        return version;
    }
}
