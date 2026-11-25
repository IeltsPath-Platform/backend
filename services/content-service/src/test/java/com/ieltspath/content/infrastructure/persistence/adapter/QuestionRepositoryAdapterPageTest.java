package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.application.usecase.ListQuestionsUseCase;
import com.ieltspath.content.application.usecase.ListQuestionsWithIncludesUseCase;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The question catalog page on the real schema: filters, ordering and totals are computed by the database. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuestionRepositoryAdapter.class, LessonAuthoringRepositoryAdapter.class, QuestionPersistenceMapper.class, JacksonAutoConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
class QuestionRepositoryAdapterPageTest {

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
    @Autowired QuestionRepository questions;
    @Autowired LessonAuthoringRepository lessonAuthoring;

    private UUID question(String skill, String purpose, String status, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,code,question_type,skill,purpose,status,created_at) "
                        + "VALUES(?,'Q-' || replace(?::text,'-',''),'MULTIPLE_CHOICE',?,?,?,?)",
                id, id, skill, purpose, status, Timestamp.from(createdAt));
        return id;
    }

    private UUID publishedVersion(UUID questionId, String stem) {
        UUID versionId = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) VALUES(?,?,1,?,'PUBLISHED')",
                versionId, questionId, stem);
        jdbc.update("UPDATE questions SET current_published_version_id = ?, status = 'PUBLISHED' WHERE id = ?",
                versionId, questionId);
        return versionId;
    }

    private UUID lessonBlock(String code) {
        UUID topicId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name) VALUES(?,?,?)", topicId, "T-" + code, code);
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order) VALUES(?,?,?,?,0)",
                lessonId, topicId, "L-" + code, code);
        jdbc.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type) VALUES(?,?,0,'EXERCISE')",
                blockId, lessonId);
        return blockId;
    }

    private UUID section() {
        UUID packageId = UUID.randomUUID();
        UUID packageVersionId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type) VALUES(?,?,?,'PRACTICE_SET')",
                packageId, "P-" + packageId, "Package");
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number,status) VALUES(?,?,1,'DRAFT')",
                packageVersionId, packageId);
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,sort_order) VALUES(?,?,?,0)",
                sectionId, packageVersionId, "Section");
        return sectionId;
    }

    private UUID knowledgePoint(String code) {
        UUID topicId = UUID.randomUUID();
        UUID pointId = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name) VALUES(?,?,?)", topicId, "T-" + code, code);
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,skill,learning_type,cefr_level) "
                        + "VALUES(?,?,?,?,'STRATEGY','ACTIVE','READING','CONCEPT','C2')",
                pointId, topicId, "KP-" + code, code);
        return pointId;
    }

    private void addQuestionPoint(UUID versionId, UUID pointId) {
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES(?,?)",
                versionId, pointId);
    }

    private long count(String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE " + where, Long.class);
    }

    @Test
    void withoutFiltersTheTotalCoversEveryQuestionAndThePageIsNewestFirst() {
        Instant base = Instant.parse("2100-01-01T00:00:00Z");
        UUID oldest = question("READING", "LEARNING", "DRAFT", base);
        UUID middle = question("READING", "LEARNING", "DRAFT", base.plusSeconds(60));
        UUID newest = question("READING", "LEARNING", "DRAFT", base.plusSeconds(120));

        QuestionRepository.Page page = questions.findPage(null, null, null, 0, 2);

        assertThat(page.totalElements()).isEqualTo(count("TRUE"));
        assertThat(page.items()).extracting(Question::getId).containsExactly(newest, middle);
        assertThat(questions.findPage(null, null, null, 1, 2).items().get(0).getId()).isEqualTo(oldest);
    }

    @Test
    void eachFilterNarrowsTheTotalAndTheItems() {
        Instant at = Instant.parse("2101-01-01T00:00:00Z");
        UUID match = question("WRITING", "EXAM", "PUBLISHED", at);
        question("WRITING", "EXAM", "DRAFT", at.plusSeconds(1));
        question("WRITING", "LEARNING", "PUBLISHED", at.plusSeconds(2));
        question("READING", "EXAM", "PUBLISHED", at.plusSeconds(3));

        QuestionRepository.Page page = questions.findPage(Skill.WRITING, QuestionPurpose.EXAM,
                PublicationStatus.PUBLISHED, 0, 10);

        assertThat(page.totalElements()).isEqualTo(count("skill = 'WRITING' AND purpose = 'EXAM' AND status = 'PUBLISHED'"));
        assertThat(page.items()).extracting(Question::getId).contains(match);
        assertThat(page.items()).allSatisfy(question -> {
            assertThat(question.getSkill()).isEqualTo(Skill.WRITING);
            assertThat(question.getPurpose()).isEqualTo(QuestionPurpose.EXAM);
            assertThat(question.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        });
        assertThat(questions.findPage(null, null, PublicationStatus.DRAFT, 0, 1).totalElements())
                .isEqualTo(count("status = 'DRAFT'"));
    }

    @Test
    void filtersQuestionsByCodeWithoutCaseSensitivity() {
        Instant at = Instant.parse("2103-01-01T00:00:00Z");
        UUID match = question("WRITING", "EXAM", "DRAFT", at);
        jdbc.update("UPDATE questions SET code = 'Q-UNIQUE-LOOKUP' WHERE id = ?", match);
        question("READING", "LEARNING", "DRAFT", at.plusSeconds(1));

        QuestionRepository.Page page = questions.findPage(null, null, null, "unique-lookup", 0, 10);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).extracting(Question::getId).containsExactly(match);
        assertThat(page.items().get(0).getCode()).isEqualTo("Q-UNIQUE-LOOKUP");
    }

    @Test
    void pageBeyondTheEndIsEmptyButKeepsTheTotal() {
        long total = count("TRUE");

        QuestionRepository.Page page = questions.findPage(null, null, null, (int) total + 5, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(total);
    }

    @Test
    void pagesDoNotOverlapAndTogetherCoverTheFilteredSet() {
        Instant at = Instant.parse("2102-01-01T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            question("SPEAKING", "PLACEMENT", "DRAFT", at.plusSeconds(i));
        }

        long total = count("skill = 'SPEAKING' AND purpose = 'PLACEMENT'");
        List<UUID> collected = new ArrayList<>();
        int pages = (int) ((total + 1) / 2);
        for (int index = 0; index < pages; index++) {
            QuestionRepository.Page page = questions.findPage(Skill.SPEAKING, QuestionPurpose.PLACEMENT, null, index, 2);
            assertThat(page.totalElements()).isEqualTo(total);
            assertThat(page.items()).hasSize(index < pages - 1 || total % 2 == 0 ? 2 : 1);
            collected.addAll(ids(page));
        }

        assertThat(collected).hasSize((int) total).doesNotHaveDuplicates();
        assertThat(questions.findPage(Skill.SPEAKING, QuestionPurpose.PLACEMENT, null, pages, 2).items()).isEmpty();
    }

    @Test
    void onlyFreeFiltersBeforePagingAndKeepsQuestionsUsedOnlyByTheExceptedBlock() {
        Instant at = Instant.parse("2104-01-01T00:00:00Z");
        UUID currentBlock = lessonBlock("CURRENT");
        UUID otherBlock = lessonBlock("OTHER");
        UUID sectionId = section();
        UUID currentVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at), "Current block");
        UUID otherVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(1)), "Other block");
        UUID sectionVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(2)), "Section");
        UUID freeVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(3)), "Free");
        question("READING", "LEARNING", "DRAFT", at.plusSeconds(4));
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,0)", currentBlock, currentVersion);
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,0)", otherBlock, otherVersion);
        jdbc.update("INSERT INTO section_questions(section_id,question_version_id,sort_order) VALUES(?,?,0)", sectionId, sectionVersion);

        QuestionRepository.Page firstFreePage = questions.findFreePage(Skill.READING, QuestionPurpose.LEARNING,
                null, "", null, 0, 1);
        QuestionRepository.Page secondFreePage = questions.findFreePage(Skill.READING, QuestionPurpose.LEARNING,
                null, "", null, 1, 1);

        assertThat(firstFreePage.totalElements()).isEqualTo(2);
        assertThat(secondFreePage.totalElements()).isEqualTo(2);
        assertThat(List.of(firstFreePage, secondFreePage).stream().flatMap(page -> page.items().stream())
                .map(Question::getCurrentPublishedVersionId)).containsExactlyInAnyOrder(freeVersion, null);

        QuestionRepository.Page exceptedBlockPage = questions.findFreePage(Skill.READING, QuestionPurpose.LEARNING,
                null, "", currentBlock, 0, 25);
        assertThat(exceptedBlockPage.totalElements()).isEqualTo(3);
        assertThat(exceptedBlockPage.items()).extracting(Question::getCurrentPublishedVersionId)
                .contains(currentVersion, freeVersion, null);
        assertThat(lessonAuthoring.questionVersionsOwnedElsewhere(currentBlock,
                List.of(currentVersion, otherVersion, sectionVersion, freeVersion)))
                .containsExactlyInAnyOrder(otherVersion, sectionVersion);
    }

    @Test
    void combinesOwnershipIncludesFreeOnlyExceptedBlockAndKnowledgePointFiltersBeforePaging() {
        Instant at = Instant.parse("2105-01-01T00:00:00Z");
        UUID point = knowledgePoint("MATCH");
        UUID otherPoint = knowledgePoint("OTHER");
        UUID currentBlock = lessonBlock("COMBINED_CURRENT");
        UUID otherBlock = lessonBlock("COMBINED_OTHER");
        UUID currentVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at), "Current");
        UUID firstFreeVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(1)), "First free");
        UUID secondFreeVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(2)), "Second free");
        UUID otherOwnerVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(3)), "Other owner");
        UUID otherPointVersion = publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(4)), "Other point");
        publishedVersion(question("READING", "LEARNING", "PUBLISHED", at.plusSeconds(5)), "Untagged");
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,0)", currentBlock, currentVersion);
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,0)", otherBlock, otherOwnerVersion);
        addQuestionPoint(currentVersion, point);
        addQuestionPoint(firstFreeVersion, point);
        addQuestionPoint(secondFreeVersion, point);
        addQuestionPoint(otherOwnerVersion, point);
        addQuestionPoint(otherPointVersion, otherPoint);

        var useCase = new ListQuestionsWithIncludesUseCase(new ListQuestionsUseCase(questions), questions, lessonAuthoring);
        var firstPage = useCase.execute(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED,
                null, 0, 1, java.util.Set.of("ownership", "preview"), true, currentBlock, List.of(point));
        var secondPage = useCase.execute(Skill.READING, QuestionPurpose.LEARNING, PublicationStatus.PUBLISHED,
                null, 1, 1, java.util.Set.of("ownership", "preview"), true, currentBlock, List.of(point));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(secondPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(3);
        assertThat(List.of(firstPage, secondPage).stream().flatMap(page -> page.items().stream())
                .map(QuestionResult::currentPublishedVersionId))
                .containsExactly(secondFreeVersion, firstFreeVersion).doesNotHaveDuplicates();
        assertThat(firstPage.items().getFirst().ownedElsewhere()).isFalse();
        assertThat(firstPage.items().getFirst().knowledgePoints()).isNotEmpty();
        assertThat(questions.findFreePageByKnowledgePoints(Skill.READING, QuestionPurpose.LEARNING,
                PublicationStatus.PUBLISHED, null, currentBlock, List.of(point), 2, 1).items())
                .extracting(Question::getCurrentPublishedVersionId).containsExactly(currentVersion);
        assertThat(questions.findFreePageByKnowledgePoints(Skill.READING, QuestionPurpose.LEARNING,
                PublicationStatus.PUBLISHED, null, currentBlock, List.of(point), 3, 1).items()).isEmpty();
    }

    private static List<UUID> ids(QuestionRepository.Page page) {
        return page.items().stream().map(Question::getId).toList();
    }
}
