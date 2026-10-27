package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.command.CourseDraftRequest;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.port.ContentGenerationPort;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.result.GenerationReply;
import com.ieltspath.content.application.service.CourseCandidateSelector;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseDraftGenerator;
import com.ieltspath.content.application.service.CourseDraftValidator;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.AiDraftConfiguration;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcAiDraftRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcAiUsageQuota;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcCourseCandidateReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcCourseCatalogReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcPracticeTestMilestoneRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.LessonAuthoringRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.LessonResourceRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.LessonResourcePersistenceMapperImpl;
import com.ieltspath.content.infrastructure.persistence.adapter.TopicRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.mapper.CoursePersistenceMapperImpl;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole AI course path on the real schema with a stand-in model: request, background run, a draft that only holds
 * catalogue content, its gaps, and approval into an inactive course. The model's reply names one point that does not
 * exist; it must not survive.
 */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AiDraftConfiguration.class, RequestCourseDraftUseCase.class, StartAiDraftUseCase.class,
        RunAiDraftJobUseCase.class, CourseDraftGenerator.class, CourseCandidateSelector.class,
        CourseDraftValidator.class, CourseDraftCodec.class, CourseDraftAnalysisService.class,
        JdbcCourseCandidateReader.class, JdbcCourseCatalogReader.class, JdbcAiDraftRepository.class,
        JdbcAiUsageQuota.class, CourseDraftUseCases.class, ApproveCourseDraftUseCase.class,
        CreateCourseUseCase.class, UpdateCourseUseCase.class, CourseRepositoryAdapter.class,
        CoursePersistenceMapperImpl.class, CreateTopicUseCase.class, UpdateTopicUseCase.class,
        TopicRepositoryAdapter.class, TopicPersistenceMapper.class, LessonAuthoringUseCase.class,
        LessonAuthoringRepositoryAdapter.class, ContentPackageRepositoryAdapter.class,
        ContentPackagePersistenceMapper.class, CreateContentPackageUseCase.class,
        CreateDraftPackageVersionUseCase.class, JdbcLearningContentReader.class,
        JdbcPracticeTestMilestoneRepository.class, CreateLessonResourceUseCase.class,
        LessonResourceRepositoryAdapter.class, LessonResourcePersistenceMapperImpl.class, CourseMaterializer.class, AiCourseDraftIntegrationTest.Beans.class})
@Testcontainers(disabledWithoutDocker = true)
class AiCourseDraftIntegrationTest {

    /** The reply the stand-in model gives: built from the prompt it receives, so it can name real codes. */
    static final AtomicReference<Function<String, String>> REPLY = new AtomicReference<>();

    @TestConfiguration
    static class Beans {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean VocabularySenseLookup senses() { return ids -> Set.copyOf(ids); }
        @Bean ContentGenerationPort model() {
            return prompt -> new GenerationReply(REPLY.get().apply(prompt), "stand-in-model");
        }
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

    private static final BigDecimal BAND = new BigDecimal("8.0"); // CEFR C2

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired RequestCourseDraftUseCase requestCourse;
    @Autowired RunAiDraftJobUseCase job;
    @Autowired AiDraftRepository drafts;
    @Autowired CourseDraftUseCases draftUseCases;
    @Autowired ApproveCourseDraftUseCase approve;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private final UUID author = UUID.randomUUID();
    private final String tag = "AI" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    private UUID kpFoundation;
    private UUID kpAdvanced;
    private String foundationCode;
    private String advancedCode;
    private UUID practiceSet;

    @BeforeEach
    void fixtures() {
        jdbc.update("DELETE FROM courses WHERE band_level = ?", BAND);
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", topic, tag + "_FIX", "Fixture",
                600_000 + (int) (Math.random() * 90_000));
        foundationCode = tag + "_BASE";
        advancedCode = tag + "_ADV";
        kpFoundation = knowledgePoint(topic, foundationCode, "Sentence structure", "C1");
        kpAdvanced = knowledgePoint(topic, advancedCode, "Paraphrase at scale", "C2");
        jdbc.update("INSERT INTO knowledge_point_prerequisites(knowledge_point_id,prerequisite_id) VALUES(?,?)",
                kpAdvanced, kpFoundation);

        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status) "
                + "VALUES(?,'MULTIPLE_CHOICE','READING','LEARNING','PUBLISHED')", question);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) "
                + "VALUES(?,?,1,'Stem','PUBLISHED')", version, question);
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES(?,?)",
                version, kpFoundation);

        practiceSet = UUID.randomUUID();
        UUID setVersion = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES(?,?,?,'PRACTICE_SET','DRAFT')",
                practiceSet, tag + "-PS", "Practice");
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) "
                + "VALUES(?,?,1,'PUBLISHED')", setVersion, practiceSet);
        UUID section = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,sort_order) VALUES(?,?,'S',1)",
                section, setVersion);
        jdbc.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES(?,?,1)",
                section, version);
        jdbc.update("UPDATE content_packages SET status='PUBLISHED', current_published_version_id=? WHERE id=?",
                setVersion, practiceSet);

        REPLY.set(prompt -> """
                {"course":{"code":"%1$s_COURSE","name":"Advanced paraphrasing"},
                 "topics":[{"code":"%1$s_T1","name":"Paraphrase","skill":"READING","lessons":[
                    {"code":"%1$s_L1","title":"Foundations","knowledgePoints":["%2$s"],
                     "practiceSets":["%1$s-PS"],"questions":["Q1"]},
                    {"code":"%1$s_L2","title":"At scale","knowledgePoints":["%3$s","%1$s_INVENTED"]}],
                   "topicTest":{"code":"%1$s_T1_TEST","title":"Topic test"}}],
                 "courseTest":{"code":"%1$s_CT","title":"Course test"}}
                """.formatted(tag, foundationCode, advancedCode));
    }

    private CourseDraftRequest request() {
        return new CourseDraftRequest(BAND, List.of(Skill.READING), List.of(), List.of(kpAdvanced), 2, null);
    }

    private AiDraft generated() {
        AiDraft queued = requestCourse.execute(author, request());
        assertThat(queued.getStatus()).isEqualTo(DraftStatus.QUEUED);
        assertThat(job.runBatch(10, Duration.ofMinutes(10))).isEqualTo(1);
        return drafts.findById(queued.getId()).orElseThrow();
    }

    @Test
    void theRunProducesAReadyDraftThatOnlyHoldsCatalogueContent() throws Exception {
        AiDraft draft = generated();

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.READY);
        assertThat(draft.getModel()).isEqualTo("stand-in-model");
        assertThat(draft.getPromptVersion()).isEqualTo("COURSE-v2");
        JsonNode result = json.readTree(draft.getResultJson());
        JsonNode lessons = result.path("topics").get(0).path("lessons");
        assertThat(lessons).hasSize(2);
        assertThat(lessons.get(0).path("knowledgePointIds").get(0).asText()).isEqualTo(kpFoundation.toString());
        assertThat(lessons.get(0).path("practiceSetPackageIds").get(0).asText()).isEqualTo(practiceSet.toString());
        assertThat(lessons.get(1).path("knowledgePointIds")).hasSize(1);
        assertThat(lessons.get(1).path("knowledgePointIds").get(0).asText()).isEqualTo(kpAdvanced.toString());
        assertThat(result.path("warnings").get(0).path("code").asText()).isEqualTo("HALLUCINATED_REFERENCE");
        assertThat(result.path("warnings").get(0).path("subject").asText()).isEqualTo(tag + "_INVENTED");
        assertThat(result.path("analysis").path("valid").asBoolean()).isTrue();
    }

    @Test
    void shortagesAreGapsAndNothingIsGeneratedForThem() throws Exception {
        int questionsBefore = jdbc.queryForObject("SELECT count(*) FROM questions", Integer.class);
        JsonNode result = json.readTree(generated().getResultJson());

        // Both points have fewer than three practice sets; only the foundation point has a question.
        List<String> reasons = new java.util.ArrayList<>();
        result.path("gaps").forEach(gap -> reasons.add(gap.path("knowledgePointId").asText() + ":"
                + gap.path("reason").asText()));
        assertThat(reasons).contains(kpFoundation + ":FEW_PRACTICE_SETS", kpAdvanced + ":NO_PRACTICE_SET",
                kpAdvanced + ":NO_EXERCISE_QUESTION");
        assertThat(result.path("gapHints")).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM questions", Integer.class)).isEqualTo(questionsBefore);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_drafts WHERE kind='QUESTIONS'", Integer.class))
                .isZero();
    }

    @Test
    void thePlanTeachesThePrerequisiteBeforeThePointThatNeedsIt() throws Exception {
        JsonNode result = json.readTree(generated().getResultJson());

        assertThat(result.path("analysis").path("errors")).isEmpty();
    }

    @Test
    void theAuthorCanApproveTheGeneratedDraftIntoAnInactiveCourse() throws Exception {
        AiDraft draft = generated();

        var approved = approve.execute(author, draft.getId());
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT status FROM courses WHERE id=?", String.class, approved.courseId()))
                .isEqualTo("INACTIVE");
        assertThat(approved.lessonIds()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE id=?", UUID.class, practiceSet))
                .isEqualTo(approved.lessonIds().get(0));
        assertThat(approved.finalTestPackageIds()).hasSize(2);
        assertThat(drafts.findById(draft.getId()).orElseThrow().getStatus()).isEqualTo(DraftStatus.APPROVED);
    }

    @Test
    void theSuccessfulRunKeepsTheAuthorsUseAndAFailedRunGivesItBack() {
        generated();
        assertThat(units()).isEqualTo(1);

        REPLY.set(prompt -> "not json at all");
        AiDraft failing = requestCourse.execute(author, request());
        job.runBatch(10, Duration.ofMinutes(10));

        AiDraft failed = drafts.findById(failing.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DraftStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo(ContentGenerationException.INVALID_REPLY);
        assertThat(units()).isEqualTo(1);
    }

    @Test
    void aRequestThatMatchesNothingFailsWithNoCandidates() {
        REPLY.set(prompt -> {
            throw new AssertionError("the model must not be asked");
        });
        AiDraft queued = requestCourse.execute(author, new CourseDraftRequest(BAND, List.of(Skill.READING),
                List.of("something nobody wrote"), List.of(), 3, null));

        job.runBatch(10, Duration.ofMinutes(10));

        AiDraft failed = drafts.findById(queued.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DraftStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo(ContentGenerationException.NO_CANDIDATES);
    }

    private int units() {
        Integer count = jdbc.queryForObject("SELECT COALESCE(sum(count), 0) FROM ai_usage_daily WHERE user_id = ?",
                Integer.class, author);
        return count == null ? 0 : count;
    }

    private UUID knowledgePoint(UUID topic, String code, String name, String level) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type,cefr_level,skill) "
                + "VALUES(?,?,?,?,'GRAMMAR','ACTIVE','CONCEPT',?,'READING')", id, topic, code, name, level);
        return id;
    }
}
