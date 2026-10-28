package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.exception.VocabularyRejectedException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.port.VocabularySenseLookup.SenseText;
import com.ieltspath.content.application.port.VocabularySenseWriter;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseDraftCodec;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.exception.CourseDraftErrorsException;
import com.ieltspath.content.domain.exception.InvalidContentStateException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcAiDraftRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Approving a course draft on the real schema: what is created, in what state, and that a failure halfway leaves
 * nothing behind. The fixture uses a CEFR level and band no seeded course owns.
 */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ApproveCourseDraftUseCase.class, CourseDraftUseCases.class, CourseMaterializer.class, CourseDraftCodec.class,
        CourseDraftAnalysisService.class, JdbcCourseCatalogReader.class, CreateCourseUseCase.class,
        UpdateCourseUseCase.class, CourseRepositoryAdapter.class, CoursePersistenceMapperImpl.class,
        CreateTopicUseCase.class, UpdateTopicUseCase.class, TopicRepositoryAdapter.class,
        TopicPersistenceMapper.class, LessonAuthoringUseCase.class, LessonAuthoringRepositoryAdapter.class,
        ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class,
        CreateContentPackageUseCase.class, CreateDraftPackageVersionUseCase.class, JdbcLearningContentReader.class,
        JdbcPracticeTestMilestoneRepository.class, JdbcAiDraftRepository.class,
        CreateLessonResourceUseCase.class, LessonResourceRepositoryAdapter.class, LessonResourcePersistenceMapperImpl.class,
        ApproveCourseDraftIntegrationTest.Beans.class})
@Testcontainers(disabledWithoutDocker = true)
class ApproveCourseDraftIntegrationTest {

    @TestConfiguration
    static class Beans {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean FakeLibrary library() { return new FakeLibrary(); }
    }

    /** The vocabulary library as the approval sees it: which senses are switched off, and the words it is given. */
    static class FakeLibrary implements VocabularySenseLookup, VocabularySenseWriter {
        final Set<UUID> inactive = new HashSet<>();
        final List<List<NewWord>> writes = new ArrayList<>();
        final Map<UUID, SenseText> texts = new HashMap<>();
        String refusal;

        @Override public Set<UUID> findActiveSenseIds(List<UUID> ids) {
            return ids.stream().filter(id -> !inactive.contains(id)).collect(Collectors.toSet());
        }

        @Override public List<SenseText> describeActiveSenses(List<UUID> ids) {
            return ids.stream().filter(id -> !inactive.contains(id) && texts.containsKey(id)).map(texts::get).toList();
        }

        @Override public Map<String, UUID> resolveOrCreate(List<NewWord> words) {
            if (refusal != null) throw new VocabularyRejectedException(refusal);
            writes.add(words);
            Map<String, UUID> senses = new LinkedHashMap<>();
            words.forEach(word -> senses.put(word.normalizedLemma(), senseOf(word.normalizedLemma())));
            return senses;
        }

        static UUID senseOf(String lemma) { return UUID.nameUUIDFromBytes(lemma.getBytes()); }
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
    @Autowired ObjectMapper json;
    @Autowired CourseDraftUseCases drafts;
    @Autowired ApproveCourseDraftUseCase approve;
    @Autowired AiDraftRepository draftStore;
    @Autowired FakeLibrary library;
    @Autowired JdbcLearningContentReader reader;
    @MockitoSpyBean CreateDraftPackageVersionUseCase draftVersions;

    private final UUID author = UUID.randomUUID();
    private final String tag = "CD" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    private UUID fixtureTopic;
    private UUID kpFoundation;
    private UUID kpAdvanced;
    private UUID practiceSet;
    private UUID practiceTest;
    private UUID questionVersion;

    @BeforeEach
    void fixtures() {
        library.inactive.clear();
        library.writes.clear();
        library.texts.clear();
        library.refusal = null;
        jdbc.update("DELETE FROM courses WHERE band_level = ?", BAND);
        fixtureTopic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", fixtureTopic, tag + "_FIXTURE",
                "Fixture", 900_000 + (int) (Math.random() * 90_000));
        kpFoundation = knowledgePoint("FOUNDATION");
        kpAdvanced = knowledgePoint("ADVANCED");
        jdbc.update("INSERT INTO knowledge_point_prerequisites(knowledge_point_id,prerequisite_id) VALUES(?,?)",
                kpAdvanced, kpFoundation);
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status,import_ref) "
                + "VALUES(?,'MULTIPLE_CHOICE','READING','LEARNING','PUBLISHED',?)", question, tag + "-Q1");
        questionVersion = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) "
                + "VALUES(?,?,1,'Stem','PUBLISHED')", questionVersion, question);
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES(?,?)",
                questionVersion, kpFoundation);
        practiceSet = publishedPackage("PRACTICE_SET", tag + "-PS");
        practiceTest = publishedPackage("PRACTICE_TEST", tag + "-PT");
    }

    // --- approving a good draft

    @Test
    void approvalCreatesInactiveCourseDraftLessonsLinkedSetsEmptyFinalTestsAndMilestones() throws Exception {
        UUID id = drafts.create(author, structure(goodDraft())).getId();

        var approved = approve.execute(author, id);
        entityManager.flush(); // the JDBC reads below must see what JPA has queued

        assertThat(jdbc.queryForObject("SELECT status FROM courses WHERE id=?", String.class, approved.courseId()))
                .isEqualTo("INACTIVE");
        assertThat(approved.topicIds()).hasSize(2);
        assertThat(jdbc.queryForList("SELECT status FROM topics WHERE course_id=? ORDER BY sort_order", String.class,
                approved.courseId())).containsExactly("INACTIVE", "INACTIVE");
        assertThat(jdbc.queryForObject("SELECT band_min FROM topics WHERE id=?", BigDecimal.class,
                approved.topicIds().get(0))).isEqualByComparingTo(BAND);

        assertThat(approved.lessonIds()).hasSize(2);
        assertThat(jdbc.queryForList("SELECT status FROM lessons WHERE topic_id IN "
                + "(SELECT id FROM topics WHERE course_id=?)", String.class, approved.courseId()))
                .containsOnly("DRAFT");
        UUID first = approved.lessonIds().get(0);
        assertThat(jdbc.queryForObject("SELECT summary_markdown FROM lessons WHERE id=?", String.class, first)).isEqualTo("- one\n- two");
        assertThat(jdbc.queryForObject("SELECT estimated_minutes FROM lessons WHERE id=?", Integer.class, first)).isEqualTo(20);
        assertThat(jdbc.queryForList("SELECT resource_type || ':' || sort_order || ':' || title FROM lesson_resources WHERE lesson_id=? "
                + "ORDER BY resource_type DESC, sort_order", String.class, first))
                .containsExactly("SLIDE:0:Deck", "DOCUMENT:0:Sheet B", "DOCUMENT:1:Sheet A");
        assertThat(jdbc.queryForList("SELECT knowledge_point_id FROM lesson_knowledge_points WHERE lesson_id=?",
                UUID.class, first)).contains(kpFoundation);
        assertThat(jdbc.queryForList("SELECT block_type FROM lesson_blocks WHERE lesson_id=? ORDER BY sort_order",
                String.class, first)).containsExactly("TEXT", "EXERCISE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lesson_block_questions WHERE question_version_id=?",
                Integer.class, questionVersion)).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE id=?", UUID.class, practiceSet))
                .isEqualTo(first);

        assertThat(approved.finalTestPackageIds()).hasSize(3);
        for (UUID finalTest : approved.finalTestPackageIds()) {
            assertThat(jdbc.queryForObject("SELECT status FROM content_packages WHERE id=?", String.class, finalTest))
                    .isEqualTo("DRAFT");
            assertThat(jdbc.queryForList("SELECT status FROM content_package_versions WHERE package_id=?",
                    String.class, finalTest)).containsExactly("DRAFT");
        }
        assertThat(jdbc.queryForList("SELECT package_type FROM content_packages WHERE topic_id IN (?, ?)",
                String.class, approved.topicIds().get(0), approved.topicIds().get(1)))
                .containsExactly("TOPIC_TEST", "TOPIC_TEST");
        assertThat(jdbc.queryForObject("SELECT package_type FROM content_packages WHERE course_id=?", String.class,
                approved.courseId())).isEqualTo("COURSE_TEST");

        assertThat(jdbc.queryForList("SELECT package_id FROM course_practice_test_milestones WHERE course_id=?",
                UUID.class, approved.courseId())).containsExactly(practiceTest);
        assertThat(draftStore.findById(id).orElseThrow().getStatus()).isEqualTo(DraftStatus.APPROVED);

        // Nothing of this reaches a learner: the course and topics are inactive, the final tests are drafts.
        assertThat(reader.topicSequence(1)).extracting(topic -> topic.topicId())
                .doesNotContainAnyElementsOf(approved.topicIds());
        assertThat(reader.publishedTestPackages(approved.topicIds().get(0))).isEmpty();
        assertThat(reader.courseTestPackages(approved.courseId())).isEmpty();
    }

    // --- vocabulary

    @Test
    void aLessonGetsOneVocabularyBlockWithThePickedAndTheProposedSenses() throws Exception {
        UUID picked = UUID.randomUUID();
        ObjectNode draft = (ObjectNode) json.readTree(goodDraft());
        ArrayNode lessons = (ArrayNode) draft.path("topics").get(0).path("lessons");
        ObjectNode first = (ObjectNode) lessons.get(0);
        first.putArray("vocabularySenseIds").add(picked.toString());
        first.putArray("newVocabulary").add(word("fluctuate")).add(word("Trend"));
        ((ObjectNode) lessons.get(1)).putArray("newVocabulary").add(word("trend"));
        UUID id = drafts.create(author, draft).getId();

        var approved = approve.execute(author, id);
        entityManager.flush();

        // The library is asked once, and a lemma two lessons propose is sent once.
        assertThat(library.writes).hasSize(1);
        assertThat(library.writes.get(0)).extracting(NewWord::normalizedLemma).containsExactly("fluctuate", "trend");
        assertThat(senseIdsOfVocabularyBlock(approved.lessonIds().get(0))).containsExactly(picked,
                FakeLibrary.senseOf("fluctuate"), FakeLibrary.senseOf("trend"));
        assertThat(senseIdsOfVocabularyBlock(approved.lessonIds().get(1))).containsExactly(FakeLibrary.senseOf("trend"));
    }

    @Test
    void theSummaryEndsWithAVocabularyCoreTableOfTheLessonsWords() throws Exception {
        UUID picked = UUID.randomUUID();
        library.texts.put(picked, new SenseText(picked, "accelerate", "tăng tốc"));
        ObjectNode draft = (ObjectNode) json.readTree(goodDraft());
        ArrayNode lessons = (ArrayNode) draft.path("topics").get(0).path("lessons");
        ObjectNode first = (ObjectNode) lessons.get(0);
        first.putArray("vocabularySenseIds").add(picked.toString());
        first.putArray("newVocabulary").add(word("fluctuate")).add(word("Accelerate"));
        UUID id = drafts.create(author, draft).getId();

        var approved = approve.execute(author, id);
        entityManager.flush();

        String summary = jdbc.queryForObject("SELECT summary_markdown FROM lessons WHERE id=?", String.class,
                approved.lessonIds().get(0));
        assertThat(summary).startsWith("- one\n- two\n\n## Vocabulary Core\n\n| Từ | Nghĩa |\n| --- | --- |\n");
        assertThat(summary).contains("| accelerate | tăng tốc |\n| fluctuate | dao dong |").doesNotContain("Accelerate");
        // A lesson with no words gets no table; this one was given no summary, so it stays without one.
        assertThat(jdbc.queryForObject("SELECT summary_markdown FROM lessons WHERE id=?", String.class,
                approved.lessonIds().get(1))).isNull();
    }

    @Test
    void aLessonWithoutVocabularyGetsNoVocabularyBlockAndTheLibraryIsNotAsked() throws Exception {
        UUID id = drafts.create(author, structure(goodDraft())).getId();

        var approved = approve.execute(author, id);
        entityManager.flush();

        assertThat(library.writes).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lesson_blocks WHERE block_type='VOCABULARY' AND lesson_id=?",
                Integer.class, approved.lessonIds().get(0))).isZero();
    }

    @Test
    void aPickedSenseThatIsNoLongerActiveIsRefusedAndNothingIsCreated() throws Exception {
        UUID gone = UUID.randomUUID();
        library.inactive.add(gone);
        ObjectNode draft = (ObjectNode) json.readTree(goodDraft());
        ((ObjectNode) draft.path("topics").get(0).path("lessons").get(0)).putArray("vocabularySenseIds")
                .add(gone.toString());
        UUID id = drafts.create(author, draft).getId();

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOfSatisfying(CourseDraftErrorsException.class,
                error -> {
                    assertThat(error.getIssues()).extracting("code").containsExactly("VOCABULARY_SENSE_UNAVAILABLE");
                    assertThat(error.getIssues()).extracting("subject").containsExactly(gone.toString());
                });
        assertThat(library.writes).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND)).isZero();
    }

    @Test
    void aWordTheLibraryRefusesIsReportedAndNothingIsCreated() throws Exception {
        library.refusal = "Vocabulary item is inactive: going";
        ObjectNode draft = (ObjectNode) json.readTree(goodDraft());
        ((ObjectNode) draft.path("topics").get(0).path("lessons").get(0)).putArray("newVocabulary").add(word("going"));
        UUID id = drafts.create(author, draft).getId();

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOfSatisfying(CourseDraftErrorsException.class,
                error -> {
                    assertThat(error.getIssues()).extracting("code").containsExactly("VOCABULARY_REJECTED");
                    assertThat(error.getIssues()).extracting("subject").containsExactly(library.refusal);
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND)).isZero();
        assertThat(draftStore.findById(id).orElseThrow().getStatus()).isEqualTo(DraftStatus.READY);
    }

    @Test
    void anApprovedDraftCannotBeApprovedAgain() throws Exception {
        UUID id = drafts.create(author, structure(goodDraft())).getId();
        approve.execute(author, id);

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOf(InvalidContentStateException.class);
    }

    @Test
    void anotherAuthorCannotApproveOrSeeTheDraft() throws Exception {
        UUID id = drafts.create(author, structure(goodDraft())).getId();

        assertThatThrownBy(() -> approve.execute(UUID.randomUUID(), id))
                .isInstanceOf(com.ieltspath.content.domain.exception.AiDraftNotFoundException.class);
        assertThatThrownBy(() -> drafts.analyze(UUID.randomUUID(), id))
                .isInstanceOf(com.ieltspath.content.domain.exception.AiDraftNotFoundException.class);
    }

    // --- refusing a bad draft

    @Test
    void aCodeThatExistsIsAConflictAndNothingIsCreated() throws Exception {
        String draft = goodDraft().replace(tag + "_COURSE", tag + "_COURSE_X");
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order,status) VALUES(?,?,?,?,1,'DRAFT')",
                UUID.randomUUID(), fixtureTopic, tag + "_L1", "Taken");
        UUID id = drafts.create(author, structure(draft)).getId();

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOfSatisfying(CourseDraftErrorsException.class,
                error -> {
                    assertThat(error.isConflict()).isTrue();
                    assertThat(error.getIssues()).extracting("code").containsOnly("CODE_TAKEN");
                    assertThat(error.getIssues()).extracting("subject").containsExactly(tag + "_L1");
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND))
                .isZero();
    }

    @Test
    void aPrerequisiteTaughtLateIsInvalidAndNothingIsCreated() throws Exception {
        ObjectNode backwards = (ObjectNode) json.readTree(goodDraft());
        ArrayNode lessons = (ArrayNode) backwards.path("topics").get(0).path("lessons");
        lessons.insert(0, lessons.remove(1)); // the advanced point's lesson comes first
        UUID id = drafts.create(author, backwards).getId();

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOfSatisfying(CourseDraftErrorsException.class,
                error -> {
                    assertThat(error.isConflict()).isFalse();
                    assertThat(error.getIssues()).extracting("code").contains("PREREQUISITE_ORDER");
                });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND))
                .isZero();
    }

    @Test
    void anEmptyDraftCannotBeApproved() {
        UUID id = drafts.create(author, null).getId();

        assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOfSatisfying(CourseDraftErrorsException.class,
                error -> assertThat(error.getIssues()).extracting("code").contains("COURSE_MISSING", "TOPIC_MISSING"));
    }

    // --- atomicity

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aFailureAfterTheLessonsLeavesNoCourseTopicLessonOrPackage() throws Exception {
        try {
            UUID id = drafts.create(author, structure(goodDraft())).getId();
            doThrow(new IllegalStateException("simulated failure")).when(draftVersions).execute(any(), any());

            assertThatThrownBy(() -> approve.execute(author, id)).isInstanceOf(IllegalStateException.class);

            assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND))
                    .isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM topics WHERE code LIKE ?", Integer.class,
                    tag + "\\_T%")).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM lessons WHERE code LIKE ?", Integer.class,
                    tag + "\\_L%")).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM content_packages WHERE code LIKE ?", Integer.class,
                    tag + "\\_%TEST")).isZero();
            assertThat(jdbc.queryForObject("SELECT lesson_id FROM content_packages WHERE id=?", UUID.class,
                    practiceSet)).isNull();
            assertThat(draftStore.findById(id).orElseThrow().getStatus()).isEqualTo(DraftStatus.READY);
        } finally {
            cleanUpCommittedFixtures();
        }
    }

    // --- analysis route

    @Test
    void analysisReportsCoverageAndGapsWithoutChangingAnything() throws Exception {
        UUID id = drafts.create(author, structure(goodDraft())).getId();

        var analysis = drafts.analyze(author, id);

        assertThat(analysis.valid()).isTrue();
        assertThat(analysis.coverage().expected()).isEqualTo(2);
        assertThat(analysis.coverage().covered()).isEqualTo(2);
        // One practice set only: below the three wanted, and the advanced point has none and no question.
        assertThat(analysis.gaps()).extracting("reason").contains("FEW_PRACTICE_SETS", "NO_PRACTICE_SET",
                "NO_EXERCISE_QUESTION");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level=?", Integer.class, BAND))
                .isZero();
    }

    // --- fixtures

    private String goodDraft() {
        return """
                {"course":{"code":"%1$s_COURSE","name":"Fixture course","bandLevel":8.5},
                 "topics":[
                  {"code":"%1$s_T1","name":"Foundations","skill":"READING","sortOrder":%2$d,
                   "lessons":[
                     {"code":"%1$s_L1","title":"Basics","summary":"s","knowledgePointIds":["%3$s"],
                      "summaryMarkdown":"- one\\n- two","estimatedMinutes":20,
                      "resources":[{"type":"SLIDE","title":"Deck","url":"https://slides.example/deck"},
                                   {"type":"DOCUMENT","title":"Sheet B","url":"https://docs.example/b"},
                                   {"type":"DOCUMENT","title":"Sheet A","url":"https://docs.example/a"}],
                      "blocks":[{"type":"TEXT","text":"Intro","knowledgePointIds":["%3$s"]},
                                {"type":"EXERCISE","questionRefs":["%1$s-Q1"]}],
                      "practiceSetPackageIds":["%5$s"]},
                     {"code":"%1$s_L2","title":"Advanced","knowledgePointIds":["%4$s"],"blocks":[]}],
                   "topicTest":{"code":"%1$s_T1_TEST","title":"Topic test"}},
                  {"code":"%1$s_T2","name":"More","skill":"READING","sortOrder":%6$d,
                   "lessons":[],"topicTest":{"code":"%1$s_T2_TEST","title":"Topic test 2"}}],
                 "courseTest":{"code":"%1$s_COURSE_TEST","title":"Course test"},
                 "practiceTestMilestones":[{"packageId":"%7$s","afterTopicSequence":1}]}
                """.formatted(tag, sortOrder(1), kpFoundation, kpAdvanced, practiceSet, sortOrder(2), practiceTest);
    }

    private ObjectNode word(String lemma) {
        return json.createObjectNode().put("lemma", lemma).put("partOfSpeech", "VERB")
                .put("englishDefinition", "to change often").put("vietnameseMeaning", "dao dong")
                .put("exampleSentence", "Prices fluctuate.");
    }

    private List<UUID> senseIdsOfVocabularyBlock(UUID lessonId) {
        return jdbc.queryForList("SELECT v.vocabulary_sense_id FROM lesson_block_vocabulary v "
                + "JOIN lesson_blocks b ON b.id = v.block_id WHERE b.lesson_id=? AND b.block_type='VOCABULARY' "
                + "ORDER BY v.sort_order", UUID.class, lessonId);
    }

    private int sortOrder(int n) {
        return 700_000 + Math.abs(tag.hashCode() % 90_000) * 2 + n;
    }

    private JsonNode structure(String text) throws Exception {
        return json.readTree(text);
    }

    private UUID knowledgePoint(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type,cefr_level,skill) "
                        + "VALUES(?,?,?,?,'GRAMMAR','ACTIVE','CONCEPT','C2','READING')",
                id, fixtureTopic, tag + "_KP_" + name, name);
        return id;
    }

    private UUID publishedPackage(String type, String code) {
        UUID id = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES(?,?,?,?,'DRAFT')",
                id, code, code, type);
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) "
                + "VALUES(?,?,1,'PUBLISHED')", version, id);
        UUID section = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,sort_order) VALUES(?,?,'S',1)",
                section, version);
        jdbc.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES(?,?,1)",
                section, questionVersion);
        jdbc.update("UPDATE content_packages SET status='PUBLISHED', current_published_version_id=? WHERE id=?",
                version, id);
        return id;
    }

    private void cleanUpCommittedFixtures() {
        jdbc.update("DELETE FROM course_practice_test_milestones WHERE package_id IN (?,?)", practiceSet, practiceTest);
        jdbc.update("DELETE FROM content_packages WHERE code LIKE ?", tag + "%");
        jdbc.update("DELETE FROM question_knowledge_points WHERE knowledge_point_id IN (?,?)", kpFoundation, kpAdvanced);
        jdbc.update("DELETE FROM questions WHERE import_ref = ?", tag + "-Q1");
        jdbc.update("DELETE FROM knowledge_point_prerequisites WHERE knowledge_point_id = ?", kpAdvanced);
        jdbc.update("DELETE FROM ai_drafts WHERE requested_by = ?", author);
        jdbc.update("DELETE FROM knowledge_points WHERE topic_id = ?", fixtureTopic);
        jdbc.update("DELETE FROM topics WHERE id = ?", fixtureTopic);
    }
}
