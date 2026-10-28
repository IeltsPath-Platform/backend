package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.exception.CourseImportInvalidException;
import com.ieltspath.content.domain.exception.ImportFileRejectedException;
import com.ieltspath.content.domain.exception.ImportQuotaExceededException;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import com.ieltspath.content.infrastructure.excel.PoiCourseWorkbookReader;
import com.ieltspath.content.infrastructure.excel.TestWorkbooks;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcAiUsageQuota;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcCourseCatalogReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcErrorTypeRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcImportCatalogReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestMilestoneRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.LessonAuthoringRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.LessonResourceRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.LessonResourcePersistenceMapperImpl;
import com.ieltspath.content.infrastructure.persistence.adapter.QuestionRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.TopicRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.mapper.CoursePersistenceMapperImpl;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.mapper.TopicPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Importing a course workbook on the real schema: what the check reports without writing, what an import creates and in
 * which state, and that a failure leaves nothing behind. The fixture uses a CEFR level and band no seeded course owns.
 */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CourseImportUseCases.class, PoiCourseWorkbookReader.class, JdbcImportCatalogReader.class,
        JdbcErrorTypeRepository.class, JdbcAiUsageQuota.class, CreateQuestionUseCase.class,
        AddQuestionVersionUseCase.class, QuestionRepositoryAdapter.class, QuestionPersistenceMapper.class,
        CreateContentSectionUseCase.class, AddSectionQuestionUseCase.class, CourseMaterializer.class,
        CreateLessonResourceUseCase.class, LessonResourceRepositoryAdapter.class, LessonResourcePersistenceMapperImpl.class,
        CourseDraftCodec.class, CourseDraftAnalysisService.class, JdbcCourseCatalogReader.class,
        CreateCourseUseCase.class, UpdateCourseUseCase.class, CourseRepositoryAdapter.class,
        CoursePersistenceMapperImpl.class, CreateTopicUseCase.class, UpdateTopicUseCase.class,
        TopicRepositoryAdapter.class, TopicPersistenceMapper.class, LessonAuthoringUseCase.class,
        LessonAuthoringRepositoryAdapter.class, ContentPackageRepositoryAdapter.class,
        ContentPackagePersistenceMapper.class, CreateContentPackageUseCase.class,
        CreateDraftPackageVersionUseCase.class, JdbcLearningContentReader.class,
        JdbcPracticeTestMilestoneRepository.class, CourseImportIntegrationTest.Beans.class})
@Testcontainers(disabledWithoutDocker = true)
class CourseImportIntegrationTest {

    @TestConfiguration
    static class Beans {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean VocabularySenseLookup senses() { return ids -> Set.copyOf(ids); }
        @Bean ContentAiProperties properties() { return new ContentAiProperties(); }
    }

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private static final BigDecimal BAND = new BigDecimal("8.5"); // CEFR C2: no seeded course or point uses it

    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired CourseImportUseCases imports;
    @Autowired ContentAiProperties properties;
    @MockitoSpyBean CreateDraftPackageVersionUseCase draftVersions;

    private final UUID author = UUID.randomUUID();
    private final String tag = "IM" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    private UUID fixtureTopic;
    private UUID existingQuestion;

    @BeforeEach
    void fixtures() {
        properties.getAi().setDailyLimit(20);
        jdbc.update("DELETE FROM courses WHERE band_level = ?", BAND);
        fixtureTopic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", fixtureTopic, tag + "_FIXTURE",
                "Fixture", 900_000 + (int) (Math.random() * 90_000));
        UUID foundation = knowledgePoint("FOUNDATION");
        UUID advanced = knowledgePoint("ADVANCED");
        jdbc.update("INSERT INTO knowledge_point_prerequisites(knowledge_point_id,prerequisite_id) VALUES(?,?)",
                advanced, foundation);
        jdbc.update("INSERT INTO error_types(code,name,category,severity,fix_hint,status,review_status) "
                + "VALUES(?,?,'READING_STRATEGY',2,'Hint','ACTIVE','APPROVED')", tag + "_ERR", "Fixture error");
        existingQuestion = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status,import_ref) "
                + "VALUES(?,'MULTIPLE_CHOICE','READING','LEARNING','PUBLISHED',?)", existingQuestion, tag + "-Q0");
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) "
                + "VALUES(?,?,1,'Stem','PUBLISHED')", version, existingQuestion);
    }

    // --- checking

    @Test
    void checkingAGoodWorkbookReportsTheCountsAndWritesNothing() {
        var report = imports.validate("course.xlsx", goodWorkbook());

        assertThat(report.errors()).isEmpty();
        assertThat(report.valid()).isTrue();
        assertThat(report.summary().topics()).isEqualTo(1);
        assertThat(report.summary().lessons()).isEqualTo(2);
        assertThat(report.summary().questions()).isEqualTo(2);
        assertThat(report.summary().packages()).isEqualTo(4);
        assertThat(report.warnings()).extracting("code").contains("FEW_PRACTICE_SETS");
        assertNothingCreated();
    }

    @Test
    void anUnknownKnowledgePointIsAnErrorWithItsRowAndNothingIsWritten() {
        byte[] file = workbook().row("Lessons", "code", tag + "_L9", "topicCode", tag + "_T1", "title", "Lost",
                "sortOrder", 9, "knowledgePointCodes", "NO_SUCH_POINT").build();

        var report = imports.validate("course.xlsx", file);

        assertThat(report.valid()).isFalse();
        assertThat(report.errors()).anySatisfy(issue -> {
            assertThat(issue.sheet()).isEqualTo("Lessons");
            assertThat(issue.row()).isEqualTo(3);
            assertThat(issue.column()).isEqualTo("knowledgePointCodes");
        });
        assertNothingCreated();
    }

    @Test
    void aFileThatIsNotAWorkbookIsRejectedBeforeItIsRead() {
        assertThatThrownBy(() -> imports.validate("course.csv", goodWorkbook()))
                .isInstanceOf(ImportFileRejectedException.class);
        assertThatThrownBy(() -> imports.validate("course.xlsx", "code,name".getBytes()))
                .isInstanceOf(ImportFileRejectedException.class);
        assertThatThrownBy(() -> imports.validate("course.xlsx", new byte[(int) CourseImportUseCases.MAX_FILE_BYTES + 1]))
                .isInstanceOf(ImportFileRejectedException.class);
    }

    // --- importing

    @Test
    void importCreatesAnInactiveCourseDraftLessonsDraftQuestionsPackagesAndTheMilestone() {
        var imported = imports.execute(author, "course.xlsx", goodWorkbook());
        entityManager.flush(); // the JDBC reads below must see what JPA has queued

        assertThat(jdbc.queryForObject("SELECT status FROM courses WHERE id=?", String.class, imported.courseId()))
                .isEqualTo("INACTIVE");
        assertThat(imported.topicIds()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT status FROM topics WHERE id=?", String.class, imported.topicIds().get(0)))
                .isEqualTo("INACTIVE");
        assertThat(imported.lessonIds()).hasSize(2);
        assertThat(jdbc.queryForList("SELECT status FROM lessons WHERE topic_id=?", String.class,
                imported.topicIds().get(0))).containsOnly("DRAFT");
        UUID first = jdbc.queryForObject("SELECT id FROM lessons WHERE code=?", UUID.class, tag + "_L1");
        assertThat(jdbc.queryForList("SELECT block_type FROM lesson_blocks WHERE lesson_id=? ORDER BY sort_order",
                String.class, first)).containsExactly("TEXT", "EXERCISE");

        assertThat(imported.questionIds()).hasSize(2);
        assertThat(jdbc.queryForList("SELECT status FROM question_versions WHERE question_id IN (SELECT id FROM "
                + "questions WHERE import_ref IN (?, ?))", String.class, tag + "-Q1", tag + "-Q2"))
                .containsExactly("DRAFT", "DRAFT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lesson_block_questions WHERE question_version_id IN "
                + "(SELECT v.id FROM question_versions v JOIN questions q ON q.id=v.question_id "
                + "WHERE q.import_ref=?)", Integer.class, tag + "-Q1")).isEqualTo(1);

        assertThat(imported.packageIds()).hasSize(4);
        assertThat(jdbc.queryForList("SELECT status FROM content_packages WHERE code LIKE ?", String.class,
                tag + "\\_%")).containsOnly("DRAFT");
        assertThat(jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE code=?", UUID.class,
                tag + "_PS1")).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT topic_id FROM content_packages WHERE code=?", UUID.class,
                tag + "_TT1")).isEqualTo(imported.topicIds().get(0));
        assertThat(jdbc.queryForObject("SELECT course_id FROM content_packages WHERE code=?", UUID.class,
                tag + "_CT1")).isEqualTo(imported.courseId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM course_practice_test_milestones m JOIN content_packages p "
                + "ON p.id=m.package_id WHERE m.course_id=? AND p.code=?", Integer.class, imported.courseId(),
                tag + "_PT1")).isEqualTo(1);
    }

    @Test
    void anInvalidWorkbookIsNotImportedAndTheErrorsComeBack() {
        byte[] file = workbook().row("Lessons", "code", tag + "_L9", "topicCode", tag + "_T1", "title", "Lost",
                "sortOrder", 9, "knowledgePointCodes", "NO_SUCH_POINT").build();

        assertThatThrownBy(() -> imports.execute(author, "course.xlsx", file))
                .isInstanceOfSatisfying(CourseImportInvalidException.class,
                        error -> assertThat(error.getErrors()).isNotEmpty());
        assertNothingCreated();
    }

    @Test
    void aCourseThatExistsAlreadyIsAConflictAndNothingIsWritten() {
        imports.execute(author, "course.xlsx", goodWorkbook());
        entityManager.flush();

        var again = imports.validate("course.xlsx", goodWorkbook());

        assertThat(again.valid()).isFalse();
        assertThat(again.errors()).extracting("code").contains("CODE_CONFLICT");
    }

    @Test
    void whenTheAuthorsAllowanceIsUsedNothingIsImported() {
        jdbc.update("INSERT INTO ai_usage_daily(user_id,usage_date,kind,count) VALUES(?,?,'IMPORT',?)", author,
                java.time.LocalDate.now(java.time.ZoneOffset.UTC), properties.getAi().getDailyLimit());

        assertThatThrownBy(() -> imports.execute(author, "course.xlsx", goodWorkbook()))
                .isInstanceOf(ImportQuotaExceededException.class);
        assertNothingCreated();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aFailureAfterTheQuestionsLeavesNoCourseQuestionOrPackage() {
        try {
            doThrow(new IllegalStateException("simulated failure")).when(draftVersions).execute(any(), any());

            assertThatThrownBy(() -> imports.execute(author, "course.xlsx", goodWorkbook()))
                    .isInstanceOf(IllegalStateException.class);

            assertNothingCreated();
        } finally {
            cleanUpCommittedFixtures();
        }
    }

    // --- fixtures

    private void assertNothingCreated() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM topics WHERE code LIKE ?", Integer.class, tag + "\\_T%"))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lessons WHERE code LIKE ?", Integer.class, tag + "\\_L%"))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM questions WHERE import_ref LIKE ?", Integer.class,
                tag + "-Q_")).isEqualTo(1); // only the fixture's own question
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_packages WHERE code LIKE ?", Integer.class,
                tag + "\\_%")).isZero();
    }

    private TestWorkbooks.Builder workbook() {
        return TestWorkbooks.workbook()
                .row("Course", "code", tag + "_C", "name", "Imported course", "bandLevel", 8.5)
                .row("Topics", "code", tag + "_T1", "name", "Foundations", "skill", "READING",
                        "sortOrder", sortOrder());
    }

    private byte[] goodWorkbook() {
        String foundation = tag + "_KP_FOUNDATION";
        String advanced = tag + "_KP_ADVANCED";
        return workbook()
                .row("Lessons", "code", tag + "_L1", "topicCode", tag + "_T1", "title", "Basics", "sortOrder", 1,
                        "knowledgePointCodes", foundation)
                .row("Lessons", "code", tag + "_L2", "topicCode", tag + "_T1", "title", "Advanced", "sortOrder", 2,
                        "knowledgePointCodes", advanced)
                .row("Blocks", "lessonCode", tag + "_L1", "sortOrder", 1, "type", "TEXT", "text", "Intro",
                        "knowledgePointCodes", foundation)
                .row("Blocks", "lessonCode", tag + "_L1", "sortOrder", 2, "type", "EXERCISE",
                        "questionRefs", tag + "-Q1")
                .row("Questions", "importRef", tag + "-Q1", "type", "MULTIPLE_CHOICE", "skill", "READING",
                        "stem", "Which one?", "optionA", "a", "optionB", "b", "optionC", "c", "answer", "b",
                        "explanation", "Because.", "difficulty", "MEDIUM", "knowledgePointCodes", foundation,
                        "distractorErrorCodes", "A=" + tag + "_ERR")
                .row("Questions", "importRef", tag + "-Q2", "type", "FILL_IN_BLANK", "skill", "READING",
                        "stem", "Fill ___", "answer", "colour | color", "explanation", "Spelling.",
                        "difficulty", "EASY", "knowledgePointCodes", foundation + "," + advanced)
                .row("PracticeSets", "packageCode", tag + "_PS1", "title", "Set 1", "lessonCode", tag + "_L1",
                        "questionRefs", tag + "-Q1," + tag + "-Q2")
                .row("Tests", "kind", "TOPIC_TEST", "packageCode", tag + "_TT1", "title", "Topic test",
                        "topicCode", tag + "_T1", "questionRefs", tag + "-Q1")
                .row("Tests", "kind", "COURSE_TEST", "packageCode", tag + "_CT1", "title", "Course test",
                        "questionRefs", tag + "-Q2")
                .row("Tests", "kind", "PRACTICE_TEST_MILESTONE", "packageCode", tag + "_PT1", "title", "Checkpoint",
                        "afterTopicSequence", 1, "questionRefs", tag + "-Q0")
                .build();
    }

    private int sortOrder() {
        return 700_000 + Math.abs(tag.hashCode() % 90_000);
    }

    private UUID knowledgePoint(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type,cefr_level,skill) "
                        + "VALUES(?,?,?,?,'GRAMMAR','ACTIVE','CONCEPT','C2','READING')",
                id, fixtureTopic, tag + "_KP_" + name, name);
        return id;
    }

    private void cleanUpCommittedFixtures() {
        jdbc.update("DELETE FROM course_practice_test_milestones WHERE package_id IN "
                + "(SELECT id FROM content_packages WHERE code LIKE ?)", tag + "\\_%");
        jdbc.update("DELETE FROM content_packages WHERE code LIKE ?", tag + "\\_%");
        jdbc.update("DELETE FROM ai_usage_daily WHERE user_id = ?", author);
        jdbc.update("DELETE FROM questions WHERE import_ref LIKE ?", tag + "-Q_");
        jdbc.update("DELETE FROM error_types WHERE code = ?", tag + "_ERR");
        jdbc.update("DELETE FROM knowledge_point_prerequisites WHERE knowledge_point_id IN "
                + "(SELECT id FROM knowledge_points WHERE topic_id = ?)", fixtureTopic);
        jdbc.update("DELETE FROM knowledge_points WHERE topic_id = ?", fixtureTopic);
        jdbc.update("DELETE FROM topics WHERE id = ?", fixtureTopic);
    }
}
