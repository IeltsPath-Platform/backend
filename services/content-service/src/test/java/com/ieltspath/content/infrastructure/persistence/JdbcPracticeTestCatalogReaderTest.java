package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestCatalogReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class JdbcPracticeTestCatalogReaderTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static DataSource dataSource;
    private Connection connection;
    private JdbcTemplate sql;
    private JdbcPracticeTestCatalogReader reader;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source = new org.postgresql.ds.PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        dataSource = source;
    }

    @BeforeEach
    void beginTransaction() throws Exception {
        connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        var isolated = new SingleConnectionDataSource(connection, true);
        sql = new JdbcTemplate(isolated);
        reader = new JdbcPracticeTestCatalogReader(new NamedParameterJdbcTemplate(isolated));
    }

    @org.junit.jupiter.api.AfterEach
    void rollback() throws Exception {
        connection.rollback();
        connection.close();
    }

    @Test
    void courseCheckpointsAreNotListedInTheLibrary() {
        List<UUID> checkpoints = sql.queryForList("SELECT package_id FROM course_practice_test_milestones", UUID.class);
        assertThat(checkpoints).as("the seeded checkpoints of both courses").hasSizeGreaterThanOrEqualTo(2);
        Fixture library = publishedTest("LIBRARY_ONLY", "Library test");

        for (Skill skill : List.of(Skill.READING, Skill.WRITING)) {
            for (String scope : List.of("FULL", "PART")) {
                var page = reader.search(new PracticeTestCatalogReader.Search(skill, scope, null,
                        List.of(), null, null, null, 0, 100));
                assertThat(page.items()).extracting(PracticeTestCardResult::packageId).doesNotContainAnyElementsOf(checkpoints);
            }
        }
        var reading = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 100));
        assertThat(reading.items()).extracting(PracticeTestCardResult::packageId).contains(library.packageId());
    }

    @Test
    void aPublishedPracticeTestWithoutASourceIsInternalAndNotListed() {
        Fixture internal = publishedTest("NO_SOURCE", "Internal test");
        sql.update("UPDATE content_packages SET source_code = NULL WHERE id = ?", internal.packageId());

        var page = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 100));

        assertThat(page.items()).extracting(PracticeTestCardResult::packageId).doesNotContain(internal.packageId());

    }

    @Test
    void aCheckpointThatIsGivenASourceStaysOutOfTheLibrary() {
        Fixture checkpoint = publishedTest("MILESTONE_WITH_SOURCE", "Checkpoint with a source");
        UUID course = sql.queryForObject("SELECT id FROM courses ORDER BY code LIMIT 1", UUID.class);
        sql.update("INSERT INTO course_practice_test_milestones(course_id, package_id, after_topic_sequence) VALUES (?, ?, 1)",
                course, checkpoint.packageId());

        var page = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 100));

        assertThat(page.items()).extracting(PracticeTestCardResult::packageId).doesNotContain(checkpoint.packageId());
    }

    @Test
    void nullQueryAndPartNumberAreAcceptedForUnrestrictedPartSearch() {
        Fixture fixture = publishedTest("NULLS", "A section without a query title");

        var page = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "PART", null,
                List.of(), null, null, null, 0, 12));

        assertThat(page.items()).anySatisfy(card -> {
            assertThat(card.packageId()).isEqualTo(fixture.packageId());
            assertThat(card.parts()).singleElement().satisfies(part -> assertThat(part.partNo()).isEqualTo(1));
        });
    }

    @Test
    void fullScopeSectionTitleMatchKeepsAllPartsAndAggregates() {
        Fixture fixture = publishedTest("FULL_FILTER", "Walking routes");
        UUID secondSection = UUID.randomUUID();
        sql.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order, part_no, time_limit_seconds) " +
                "VALUES (?, ?, 'Quiet streets', 'READING', 2, 2, 600)", secondSection, fixture.versionId());
        addQuestion(secondSection, 1, "MULTIPLE_CHOICE");
        addQuestion(secondSection, 2, "MULTIPLE_CHOICE");
        addQuestion(secondSection, 3, "MULTIPLE_CHOICE");
        addGroup(secondSection, "GAP_FILLING", 1);

        var page = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), "Walking", null, null, 0, 12));

        var card = page.items().stream().filter(item -> item.packageId().equals(fixture.packageId())).findFirst().orElseThrow();
        assertThat(card.questionCount()).isEqualTo(4);
        assertThat(card.timeLimitSeconds()).isEqualTo(1200);
        assertThat(card.parts()).extracting(PracticeTestCardResult.Part::partNo).containsExactly(1, 2);
        assertThat(card.parts()).extracting(PracticeTestCardResult.Part::questionCount).containsExactly(1, 3);
    }

    @Test
    void formatFilterKeepsOnlyTestsContainingAListedFormat() {
        Fixture fixture = publishedTest("FORMATS", "Format filtering");

        var matching = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "PART", null,
                List.of(), null, null, null, 0, 50, List.of("MULTIPLE_CHOICE_ONE", "MATCHING_HEADINGS")));
        var fullMatching = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 50, List.of("MULTIPLE_CHOICE_ONE")));
        var other = reader.search(new PracticeTestCatalogReader.Search(Skill.READING, "PART", null,
                List.of(), null, null, null, 0, 50, List.of("MATCHING_HEADINGS")));

        assertThat(matching.items()).extracting(PracticeTestCardResult::packageId).contains(fixture.packageId());
        assertThat(fullMatching.items()).extracting(PracticeTestCardResult::packageId).contains(fixture.packageId());
        assertThat(other.items()).extracting(PracticeTestCardResult::packageId).doesNotContain(fixture.packageId());
        assertThat(matching.totalElements()).isGreaterThanOrEqualTo(other.totalElements());
    }

    @Test
    void listsTheFormatsOfEverySkillThatHasPublishedPracticeTests() {
        publishedTest("FORMAT_LIST", "Format list");

        assertThat(reader.listFormats(Skill.READING)).extracting(PracticeTestCatalogReader.FormatCount::code)
                .contains("MULTIPLE_CHOICE_ONE", "TRUE_FALSE_NOT_GIVEN");
        assertThat(reader.listFormats(Skill.LISTENING)).extracting(PracticeTestCatalogReader.FormatCount::code)
                .contains("GAP_FILLING", "MATCHING", "MULTIPLE_CHOICE_ONE");
        assertThat(reader.listFormats(Skill.WRITING)).extracting(PracticeTestCatalogReader.FormatCount::code)
                .contains("LINE_GRAPH", "OPINION_ESSAY");
        assertThat(reader.listFormats(Skill.SPEAKING)).extracting(PracticeTestCatalogReader.FormatCount::code)
                .contains("TECHNOLOGY", "HOMETOWN");
    }

    private Fixture publishedTest(String codeSuffix, String sectionTitle) {
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        sql.update("INSERT INTO content_packages(id, code, title, package_type, status, source_code) " +
                        "VALUES (?, ?, 'Practice package', 'PRACTICE_TEST', 'PUBLISHED', 'IELTS_SPACE_PRO')",
                packageId, "PT_CATALOG_" + codeSuffix + "_" + packageId.toString().substring(0, 8));
        sql.update("INSERT INTO content_package_versions(id, package_id, version_number, status, published_at) " +
                "VALUES (?, ?, 1, 'PUBLISHED', CURRENT_TIMESTAMP)", versionId, packageId);
        sql.update("UPDATE content_packages SET current_published_version_id = ? WHERE id = ?", versionId, packageId);
        sql.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order, part_no, time_limit_seconds) " +
                "VALUES (?, ?, ?, 'READING', 1, 1, 600)", sectionId, versionId, sectionTitle);
        addQuestion(sectionId, 1, "MULTIPLE_CHOICE");
        addGroup(sectionId, "MULTIPLE_CHOICE_ONE", 1);
        return new Fixture(packageId, versionId, sectionId);
    }

    private void addQuestion(UUID sectionId, int order, String questionType) {
        UUID questionId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        sql.update("INSERT INTO questions(id, question_type, skill, purpose, status) VALUES (?, ?, 'READING', 'EXAM', 'PUBLISHED')",
                questionId, questionType);
        sql.update("INSERT INTO question_versions(id, question_id, version_number, stem, answer_spec, status) " +
                        "VALUES (?, ?, 1, 'Question', '{}'::jsonb, 'PUBLISHED')", questionVersionId, questionId);
        sql.update("INSERT INTO section_questions(section_id, question_version_id, sort_order) VALUES (?, ?, ?)",
                sectionId, questionVersionId, order);
    }

    private void addGroup(UUID sectionId, String format, int order) {
        sql.update("INSERT INTO section_question_groups(section_id, sort_order, format_code, instructions) VALUES (?, ?, ?, 'Read the passage')",
                sectionId, order, format);
    }

    private record Fixture(UUID packageId, UUID versionId, UUID sectionId) {}

}
