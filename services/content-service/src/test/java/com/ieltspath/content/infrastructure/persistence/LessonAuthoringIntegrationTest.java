package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.usecase.LessonAuthoringUseCase;
import com.ieltspath.content.domain.exception.LessonExerciseLockedException;
import com.ieltspath.content.domain.exception.LessonNotPublishableException;
import com.ieltspath.content.domain.exception.QuestionAlreadyOwnedException;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.vo.BlockType;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class LessonAuthoringIntegrationTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static DataSource dataSource;
    private static JdbcTemplate jdbc;
    private static LessonAuthoringRepository repository;
    private static LessonAuthoringUseCase useCase;
    private final String fixturePrefix = "LT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private UUID topicId;
    private UUID pointId;
    private final List<UUID> questionIds = new ArrayList<>();
    private final List<UUID> lessonIds = new ArrayList<>();

    @BeforeAll static void migrate() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl()); source.setUser(POSTGRES.getUsername()); source.setPassword(POSTGRES.getPassword());
        dataSource = source;
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        repository = new com.ieltspath.content.infrastructure.persistence.adapter.LessonAuthoringRepositoryAdapter(
                jdbc, new NamedParameterJdbcTemplate(source));
        VocabularySenseLookup senses = ids -> Set.copyOf(ids);
        useCase = new LessonAuthoringUseCase(repository, senses);
    }

    @BeforeEach void createTopic() {
        topicId = UUID.randomUUID(); pointId = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name) VALUES(?,?,?)", topicId, fixturePrefix + "_TOPIC", "Lesson authoring fixture");
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type) VALUES(?,?,?,?, 'GRAMMAR','ACTIVE','CONCEPT')",
                pointId, topicId, fixturePrefix + "_KP", "Fixture knowledge point");
    }

    @AfterEach void cleanFixtures() {
        for (UUID id : lessonIds) jdbc.update("DELETE FROM lessons WHERE id=?", id);
        for (UUID id : questionIds) jdbc.update("DELETE FROM questions WHERE id=?", id);
        if (topicId != null) jdbc.update("DELETE FROM topics WHERE id=?", topicId);
        lessonIds.clear(); questionIds.clear();
    }

    @AfterAll static void closeContainer() { if (POSTGRES.isRunning()) POSTGRES.stop(); }

    @Test void createsLessonAndAllBlockTypesWithStableIds() {
        var lesson = createLesson("ALL_BLOCKS");
        UUID assetId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets(id,asset_type,text_content,validation_status) VALUES(?,'IMAGE','asset','VALID')", assetId);
        UUID questionId = question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}");
        var afterText = useCase.addBlock(lesson.id(), BlockType.TEXT, 0, "Lesson text", null, List.of(), List.of(), List.of(pointId));
        UUID textId = afterText.blocks().get(0).id();
        var afterAsset = useCase.addBlock(lesson.id(), BlockType.ASSET, 1, null, assetId, List.of(), List.of(), List.of());
        UUID assetBlockId = afterAsset.blocks().stream().filter(b -> b.blockType() == BlockType.ASSET).findFirst().orElseThrow().id();
        UUID senseId = UUID.randomUUID();
        var afterVocab = useCase.addBlock(lesson.id(), BlockType.VOCABULARY, 2, null, null, List.of(senseId), List.of(), List.of());
        UUID vocabBlockId = afterVocab.blocks().stream().filter(b -> b.blockType() == BlockType.VOCABULARY).findFirst().orElseThrow().id();
        var result = useCase.addBlock(lesson.id(), BlockType.EXERCISE, 3, null, null, List.of(), List.of(questionId), List.of());

        assertThat(result.blocks()).hasSize(4);
        assertThat(result.blocks()).extracting("blockType").containsExactly(BlockType.TEXT, BlockType.ASSET, BlockType.VOCABULARY, BlockType.EXERCISE);
        assertThat(result.blocks().get(0).id()).isEqualTo(textId);
        assertThat(result.blocks().get(0).textContent()).isEqualTo("Lesson text");
        assertThat(result.blocks().get(1).id()).isEqualTo(assetBlockId);
        assertThat(result.blocks().get(1).assetId()).isEqualTo(assetId);
        assertThat(result.blocks().get(2).id()).isEqualTo(vocabBlockId);
        assertThat(result.blocks().get(2).vocabularySenseIds()).containsExactly(senseId);
        assertThat(result.blocks().get(3).questions()).extracting("id").containsExactly(questionId);
    }

    @Test void reordersBlocksAroundNegativeVocabularyOrder() {
        var lesson = createLesson("NEGATIVE_ORDER");
        var first = useCase.addBlock(lesson.id(), BlockType.TEXT, 0, "text", null, List.of(), List.of(), List.of());
        UUID textId = first.blocks().get(0).id();
        UUID senseId = UUID.randomUUID();
        var second = useCase.addBlock(lesson.id(), BlockType.VOCABULARY, 1, null, null, List.of(senseId), List.of(), List.of());
        UUID vocabId = second.blocks().stream().filter(b -> b.blockType() == BlockType.VOCABULARY).findFirst().orElseThrow().id();
        jdbc.update("UPDATE lesson_blocks SET sort_order=-5 WHERE id=?", vocabId);

        useCase.reorderBlocks(lesson.id(), List.of(vocabId, textId));
        assertThat(useCase.get(lesson.id()).blocks()).extracting("id").containsExactly(vocabId, textId);
    }

    @Test void editsPublishedTextWithoutChangingIdAndLocksExerciseChanges() {
        var lesson = createLesson("PUBLISHED_EDIT");
        var withText = useCase.addBlock(lesson.id(), BlockType.TEXT, 0, "before", null, List.of(), List.of(), List.of());
        UUID textId = withText.blocks().get(0).id();
        UUID qid = question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}");
        var withExercise = useCase.addBlock(lesson.id(), BlockType.EXERCISE, 1, null, null, List.of(), List.of(qid), List.of());
        UUID exerciseId = withExercise.blocks().stream().filter(b -> b.blockType() == BlockType.EXERCISE).findFirst().orElseThrow().id();
        useCase.knowledgePoints(lesson.id(), List.of(pointId));
        useCase.publish(lesson.id());

        var updated = useCase.updateBlock(lesson.id(), textId, BlockType.TEXT, null, "after", null, List.of(), List.of(), List.of(pointId));
        assertThat(updated.blocks().stream().filter(b -> b.id().equals(textId)).findFirst().orElseThrow().textContent()).isEqualTo("after");
        assertThatThrownBy(() -> useCase.updateBlock(lesson.id(), exerciseId, BlockType.EXERCISE, null, null, null, List.of(), List.of(UUID.randomUUID()), List.of()))
                .isInstanceOf(LessonExerciseLockedException.class);
        assertThatThrownBy(() -> useCase.deleteBlock(lesson.id(), exerciseId)).isInstanceOf(LessonExerciseLockedException.class);
        assertThatThrownBy(() -> useCase.addBlock(lesson.id(), BlockType.EXERCISE, null, null, null, List.of(), List.of(UUID.randomUUID()), List.of()))
                .isInstanceOf(LessonExerciseLockedException.class);
    }

    @Test void rejectsLessonWithoutActiveKnowledgePointOrBlocks() {
        var lesson = createLesson("NO_BLOCKS");
        assertThatThrownBy(() -> useCase.publish(lesson.id())).isInstanceOf(LessonNotPublishableException.class);
        useCase.addBlock(lesson.id(), BlockType.TEXT, null, "text", null, List.of(), List.of(), List.of());
        useCase.knowledgePoints(lesson.id(), List.of());
        assertThatThrownBy(() -> useCase.publish(lesson.id())).isInstanceOf(LessonNotPublishableException.class);
    }

    @Test void validatesKnowledgePointUuidArrayAgainstPostgres() {
        var lesson = createLesson("UUID_ARRAY");
        useCase.knowledgePoints(lesson.id(), List.of(pointId));
        assertThat(useCase.get(lesson.id()).knowledgePointIds()).containsExactly(pointId);
    }

    @Test void rejectsQuestionPublicationAndOwnershipViolations() {
        var draftLesson = createLesson("DRAFT_QUESTION");
        UUID draftQuestion = question("MULTIPLE_CHOICE", "LEARNING", "DRAFT", "{}");
        useCase.addBlock(draftLesson.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(draftQuestion), List.of());
        useCase.knowledgePoints(draftLesson.id(), List.of(pointId));
        assertThatThrownBy(() -> useCase.publish(draftLesson.id())).isInstanceOf(LessonNotPublishableException.class);

        var purposeLesson = createLesson("WRONG_PURPOSE");
        UUID wrongPurpose = question("MULTIPLE_CHOICE", "PLACEMENT", "PUBLISHED", "{}");
        useCase.addBlock(purposeLesson.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(wrongPurpose), List.of());
        useCase.knowledgePoints(purposeLesson.id(), List.of(pointId));
        assertThatThrownBy(() -> useCase.publish(purposeLesson.id())).isInstanceOf(LessonNotPublishableException.class);

        var ownerOne = createLesson("OWNER_ONE"); var ownerTwo = createLesson("OWNER_TWO");
        UUID reused = question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}");
        useCase.addBlock(ownerOne.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(reused), List.of());
        // A question has one home: a second lesson cannot take it, and nothing is written for the refused block.
        assertThatThrownBy(() -> useCase.addBlock(ownerTwo.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(reused), List.of()))
                .isInstanceOf(QuestionAlreadyOwnedException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lesson_blocks WHERE lesson_id=?", Integer.class, ownerTwo.id())).isZero();
        useCase.knowledgePoints(ownerOne.id(), List.of(pointId));
        useCase.publish(ownerOne.id());
    }

    @Test void publicationStillRefusesAQuestionThatTwoLessonsHoldBecauseOfOlderData() {
        var ownerOne = createLesson("OLD_ONE"); var ownerTwo = createLesson("OLD_TWO");
        UUID reused = question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}");
        useCase.addBlock(ownerOne.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(reused), List.of());
        useCase.addBlock(ownerTwo.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}")), List.of());
        UUID otherBlock = jdbc.queryForObject("SELECT id FROM lesson_blocks WHERE lesson_id=?", UUID.class, ownerTwo.id());
        // Data that predates the rule: the same question also sits in a second lesson's block.
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,5)", otherBlock, reused);
        useCase.knowledgePoints(ownerOne.id(), List.of(pointId));

        assertThatThrownBy(() -> useCase.publish(ownerOne.id())).isInstanceOf(LessonNotPublishableException.class);
    }

    @Test void rejectsPublishedPackageQuestionOwner() {
        var lesson = createLesson("PACKAGE_OWNER");
        UUID questionId = question("MULTIPLE_CHOICE", "LEARNING", "PUBLISHED", "{}");
        useCase.addBlock(lesson.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(questionId), List.of());
        useCase.knowledgePoints(lesson.id(), List.of(pointId));
        UUID packageId = UUID.randomUUID(), versionId = UUID.randomUUID(), sectionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,status) VALUES(?,?,?,'QUIZ','PUBLISHED')", packageId, fixturePrefix + "_PKG", "Package");
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES(?,?,1,'PUBLISHED')", versionId, packageId);
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) VALUES(?,?,?,'READING',0)", sectionId, versionId, "Section");
        jdbc.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES(?,?,0)", sectionId, questionId);
        assertThatThrownBy(() -> useCase.publish(lesson.id())).isInstanceOf(LessonNotPublishableException.class);
    }

    @Test void rejectsEssaysWithMissingTaskOrTaskOneChartRequirements() {
        rejectEssay("ESSAY_NO_TASK", "{\"type\":\"ESSAY\",\"passBand\":5.5}");
        rejectEssay("ESSAY_NO_FACTS", "{\"type\":\"ESSAY\",\"task\":\"TASK_1\",\"passBand\":5.5}");
    }

    private void rejectEssay(String code, String answerSpec) {
        var lesson = createLesson(code);
        UUID questionId = question("ESSAY", "LEARNING", "PUBLISHED", answerSpec);
        useCase.addBlock(lesson.id(), BlockType.EXERCISE, 0, null, null, List.of(), List.of(questionId), List.of());
        useCase.knowledgePoints(lesson.id(), List.of(pointId));
        assertThatThrownBy(() -> useCase.publish(lesson.id())).isInstanceOf(LessonNotPublishableException.class);
    }

    private com.ieltspath.content.application.result.LessonAuthoringResult createLesson(String suffix) {
        var lesson = useCase.create(topicId, fixturePrefix + "_" + suffix, suffix, null, null, null, null);
        lessonIds.add(lesson.id()); return lesson;
    }

    private UUID question(String type, String purpose, String status, String answerSpec) {
        UUID qid = UUID.randomUUID(), versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status) VALUES(?,?, 'READING', ?, 'DRAFT')", qid, type, purpose);
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,answer_spec,status) VALUES(?,?,1,'Fixture stem',?::jsonb,?)", versionId, qid, answerSpec, status);
        questionIds.add(qid); return versionId;
    }
}
