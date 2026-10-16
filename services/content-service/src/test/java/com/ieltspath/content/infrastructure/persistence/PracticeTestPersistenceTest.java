package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.command.PublishContentPackageCommand;
import com.ieltspath.content.application.port.PracticeTestCatalogReader;
import com.ieltspath.content.application.result.PracticeTestCardResult;
import com.ieltspath.content.application.usecase.ListContentPackagesUseCase;
import com.ieltspath.content.application.usecase.PublishContentPackageUseCase;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestCatalogReader;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
class PracticeTestPersistenceTest {
    private static final List<String> DEMO_CODES = List.of(
            "PT-READING-ISP-01", "PT-READING-C10-01", "PT-READING-ACT-01");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ContentPackageRepositoryAdapter packages;

    @Test
    void v30PackagesMapThroughRepositoryAndAdminListWithPartsCoversAndGroupIds() {
        var pkg = packages.findByCode("PT-READING-ISP-01").orElseThrow();

        assertThat(pkg.getPackageType().name()).isEqualTo("PRACTICE_TEST");
        assertThat(pkg.getSourceCode()).isEqualTo("IELTS_SPACE_PRO");
        var version = pkg.getVersions().stream()
                .filter(item -> item.getId().equals(pkg.getCurrentPublishedVersionId())).findFirst().orElseThrow();
        assertThat(version.getSections()).hasSize(3);
        assertThat(version.getSections()).extracting(section -> section.getPartNo()).containsExactly(1, 2, 3);
        assertThat(version.getSections()).allSatisfy(section -> {
            assertThat(section.getCoverAssetId()).isNotNull();
            assertThat(section.getQuestions()).isNotEmpty();
            assertThat(section.getQuestions()).allSatisfy(question -> assertThat(question.getGroupId()).isNotNull());
        });

        var adminIsp = new ListContentPackagesUseCase(packages).execute(null, PublicationStatus.PUBLISHED).stream()
                .filter(item -> item.code().equals("PT-READING-ISP-01")).findFirst().orElseThrow();
        assertThat(adminIsp.packageType().name()).isEqualTo("PRACTICE_TEST");
        assertThat(adminIsp.sourceCode()).isEqualTo("IELTS_SPACE_PRO");
    }

    @Test
    void v30PackagesPassPublishingRulesAndReserveTheirQuestionVersions() {
        var namedJdbc = new NamedParameterJdbcTemplate(jdbc.getDataSource());
        var learning = new JdbcLearningContentReader(namedJdbc);
        var catalog = new JdbcPracticeTestCatalogReader(namedJdbc);
        var questionVersionIds = jdbc.query("""
                        SELECT DISTINCT qv.id
                        FROM content_packages p
                        JOIN content_package_versions v ON v.id = p.current_published_version_id
                        JOIN content_sections s ON s.package_version_id = v.id
                        JOIN section_questions sq ON sq.section_id = s.id
                        JOIN question_versions qv ON qv.id = sq.question_version_id
                        WHERE p.code IN (?, ?, ?)
                        """, (rs, row) -> rs.getObject(1, UUID.class), DEMO_CODES.toArray());

        assertThat(learning.questionVersionsReservedForLearning(questionVersionIds)).containsAll(questionVersionIds);
        var page = catalog.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 50));
        assertThat(page.items()).extracting(PracticeTestCardResult::packageCode).containsAll(DEMO_CODES);

        var publisher = new PublishContentPackageUseCase(packages, learning);
        for (String code : DEMO_CODES) {
            var pkg = packages.findByCode(code).orElseThrow();
            publisher.execute(new PublishContentPackageCommand(pkg.getId(), pkg.getCurrentPublishedVersionId(), UUID.randomUUID()));
        }

        UUID targetVersion = packages.findByCode("PT-READING-ISP-01").orElseThrow().getCurrentPublishedVersionId();
        assertThat(learning.practiceTestPublishIssues(targetVersion)).isEmpty();
        assertThat(learning.questionsUsedElsewhere(targetVersion)).isEmpty();
    }

    @Test
    void eachV30PracticeTestIsRecognizedAsAnOwnerWhenAnotherPackageReusesItsQuestions() {
        var learning = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(jdbc.getDataSource()));
        for (int index = 0; index < DEMO_CODES.size(); index++) {
            String demoCode = DEMO_CODES.get(index);
            UUID demoVersion = jdbc.queryForObject("""
                    SELECT v.id FROM content_packages p
                    JOIN content_package_versions v ON v.id = p.current_published_version_id
                    WHERE p.code = ?
                    """, UUID.class, demoCode);
            UUID questionVersion = jdbc.queryForObject("""
                    SELECT sq.question_version_id FROM content_sections s
                    JOIN section_questions sq ON sq.section_id = s.id
                    WHERE s.package_version_id = ? ORDER BY s.sort_order, sq.sort_order LIMIT 1
                    """, UUID.class, demoVersion);
            UUID competitorPackage = UUID.randomUUID();
            UUID competitorVersion = UUID.randomUUID();
            UUID competitorSection = UUID.randomUUID();
            String competitorCode = "PT-COMPETITOR-" + index;
            jdbc.update("INSERT INTO content_packages(id, code, title, package_type, status, source_code) " +
                            "VALUES (?, ?, 'Competing package', 'PRACTICE_TEST', 'PUBLISHED', 'OTHER')",
                    competitorPackage, competitorCode);
            jdbc.update("INSERT INTO content_package_versions(id, package_id, version_number, status) " +
                            "VALUES (?, ?, 1, 'PUBLISHED')", competitorVersion, competitorPackage);
            jdbc.update("UPDATE content_packages SET current_published_version_id = ? WHERE id = ?",
                    competitorVersion, competitorPackage);
            jdbc.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order, part_no) " +
                            "VALUES (?, ?, 'Reused questions', 'READING', 1, 1)",
                    competitorSection, competitorVersion);
            jdbc.update("INSERT INTO section_questions(section_id, question_version_id, sort_order) VALUES (?, ?, 1)",
                    competitorSection, questionVersion);

            var conflicts = learning.questionsUsedElsewhere(demoVersion);
            assertThat(conflicts).anySatisfy(conflict -> {
                assertThat(conflict.ownerType().name()).isEqualTo("PACKAGE");
                assertThat(conflict.ownerCode()).isEqualTo(competitorCode);
            });
        }
    }

    @Test
    void publishValidationReportsMissingPartsMixedSkillsAndUngroupedQuestions() {
        var learning = new JdbcLearningContentReader(new NamedParameterJdbcTemplate(jdbc.getDataSource()));
        UUID packageId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID readingSection = UUID.randomUUID();
        UUID listeningSection = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id, code, title, package_type, status, source_code) " +
                        "VALUES (?, 'PT_INVALID_STRUCTURE', 'Invalid practice', 'PRACTICE_TEST', 'DRAFT', 'OTHER')",
                packageId);
        jdbc.update("INSERT INTO content_package_versions(id, package_id, version_number, status) VALUES (?, ?, 1, 'DRAFT')",
                versionId, packageId);
        jdbc.update("INSERT INTO content_sections(id, package_version_id, title, skill, sort_order, part_no) " +
                        "VALUES (?, ?, 'Reading', 'READING', 1, NULL), (?, ?, 'Listening', 'LISTENING', 2, 1)",
                readingSection, versionId, listeningSection, versionId);
        jdbc.update("INSERT INTO questions(id, question_type, skill, purpose, status) VALUES (?, 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED')",
                questionId);
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, answer_spec, status) " +
                        "VALUES (?, ?, 1, 'Question', '{\"type\":\"CHOICE\",\"correct\":\"A\"}'::jsonb, 'PUBLISHED')",
                questionVersionId, questionId);
        jdbc.update("INSERT INTO section_questions(section_id, question_version_id, sort_order) VALUES (?, ?, 1)",
                readingSection, questionVersionId);

        assertThat(learning.practiceTestPublishIssues(versionId))
                .contains("Practice tests require unique part numbers and one supported skill per section",
                        "Every practice test section and question must belong to a question group");
    }

    @Test
    void v30CatalogHasExpectedNumericAndFilterResultsAndKeepsSvgCovers() {
        var catalog = new JdbcPracticeTestCatalogReader(new NamedParameterJdbcTemplate(jdbc.getDataSource()));

        var allParts = catalog.search(new PracticeTestCatalogReader.Search(Skill.READING, "PART", null,
                List.of(), null, null, null, 0, 50));
        // Later migrations add more Reading tests, so only the three V30 packages are counted here.
        var v30Codes = List.of("PT-READING-ISP-01", "PT-READING-C10-01", "PT-READING-ACT-01");
        assertThat(allParts.items().stream().filter(card -> v30Codes.contains(card.packageCode()))).hasSize(6);
        var allTests = catalog.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), null, null, null, 0, 50));
        assertThat(allTests.items().stream().filter(card -> v30Codes.contains(card.packageCode()))).hasSize(3);

        var isp = allTests.items().stream().filter(card -> card.packageCode().equals("PT-READING-ISP-01"))
                .findFirst().orElseThrow();
        assertThat(isp.questionCount()).isEqualTo(15);
        assertThat(isp.timeLimitSeconds()).isEqualTo(3600);
        assertThat(isp.parts()).hasSize(3);
        assertThat(isp.coverReference()).startsWith("data:image/svg+xml;base64,");
        assertThat(isp.parts().getFirst().formats()).containsExactly("TRUE_FALSE_NOT_GIVEN", "GAP_FILLING");

        assertThat(catalog.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of("C10_C20"), null, null, null, 0, 50)).items())
                .extracting(PracticeTestCardResult::packageCode).contains("PT-READING-C10-01")
                .doesNotContain("PT-READING-ISP-01", "PT-READING-ACT-01");
        assertThat(catalog.search(new PracticeTestCatalogReader.Search(Skill.READING, "FULL", null,
                List.of(), "walking", null, null, 0, 50)).items())
                .extracting(PracticeTestCardResult::packageCode).containsExactly("PT-READING-C10-01");
        assertThat(allParts.items().stream().filter(card -> card.packageCode().equals("PT-READING-ACT-01")))
                .hasSize(2).allSatisfy(card -> assertThat(card.requiredFeatureKey()).isEqualTo("PREMIUM_CONTENT"));
    }
}
