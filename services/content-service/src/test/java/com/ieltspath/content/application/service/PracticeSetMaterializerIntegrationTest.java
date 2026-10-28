package com.ieltspath.content.application.service;

import com.ieltspath.content.application.usecase.AddQuestionVersionUseCase;
import com.ieltspath.content.application.usecase.AddSectionQuestionUseCase;
import com.ieltspath.content.application.usecase.CreateContentPackageUseCase;
import com.ieltspath.content.application.usecase.CreateContentSectionUseCase;
import com.ieltspath.content.application.usecase.CreateDraftPackageVersionUseCase;
import com.ieltspath.content.application.usecase.CreateQuestionUseCase;
import com.ieltspath.content.application.usecase.SectionMediaUseCases;
import com.ieltspath.content.domain.vo.CourseDraftStructure.OptionPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.PracticeSetPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.QuestionPlan;
import com.ieltspath.content.domain.vo.MediaReferencePolicy;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcErrorTypeRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcSectionMediaWriter;
import com.ieltspath.content.infrastructure.persistence.adapter.QuestionRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A proposed practice set created on the real schema: what exists afterwards, and in which state. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PracticeSetMaterializer.class, CreateContentPackageUseCase.class, CreateDraftPackageVersionUseCase.class,
        CreateContentSectionUseCase.class, SectionMediaUseCases.class, CreateQuestionUseCase.class,
        AddQuestionVersionUseCase.class, AddSectionQuestionUseCase.class, QuestionRepositoryAdapter.class,
        QuestionPersistenceMapper.class, ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class,
        JdbcErrorTypeRepository.class, JdbcSectionMediaWriter.class, JdbcLearningContentReader.class,
        PracticeSetMaterializerIntegrationTest.Beans.class})
@Testcontainers(disabledWithoutDocker = true)
class PracticeSetMaterializerIntegrationTest {

    @TestConfiguration
    static class Beans {
        @Bean MediaReferencePolicy mediaReferencePolicy() { return new MediaReferencePolicy("https://media.example.test/ieltspath"); }
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper() { return new com.fasterxml.jackson.databind.ObjectMapper(); }
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

    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired PracticeSetMaterializer materializer;

    private final String tag = "PS" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

    @Test
    void createsADraftSetWithItsPassageAndDraftQuestionsLinkedToThePointAndAttachedToTheLesson() {
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", topic, tag + "_T", "Fixture",
                900_000 + (int) (Math.random() * 90_000));
        UUID lesson = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order) VALUES(?,?,?,?,1)", lesson, topic, tag + "_L", "Lesson");
        UUID point = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type,cefr_level,skill) "
                + "VALUES(?,?,?,?,'GRAMMAR','ACTIVE','CONCEPT','C2','READING')", point, topic, tag + "_KP", "Topic sentence");
        PracticeSetPlan plan = new PracticeSetPlan("Topic sentences", "Read and answer.", "A. Gardens help.\nB. They cost little.",
                QuestionType.MULTIPLE_CHOICE, List.of(point), List.of(
                question("Which sentence states the idea?", "A"), question("What does B add?", "B"), question("Why plant gardens?", "A")));

        UUID created = materializer.create(lesson, tag + "_L", 1, plan);
        entityManager.flush();

        assertThat(jdbc.queryForMap("SELECT code, package_type, status, lesson_id FROM content_packages WHERE id=?", created))
                .containsEntry("code", "PS-" + tag + "_L-1").containsEntry("package_type", "PRACTICE_SET")
                .containsEntry("status", "DRAFT").containsEntry("lesson_id", lesson);
        assertThat(jdbc.queryForObject("SELECT status FROM content_package_versions WHERE package_id=?", String.class, created))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_sections s JOIN content_package_versions v ON v.id=s.package_version_id "
                + "WHERE v.package_id=? AND s.skill='READING' AND s.instructions='Read and answer.'", Integer.class, created)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT a.text_content FROM content_assets a JOIN content_asset_links l ON l.asset_id=a.id "
                + "JOIN content_sections s ON s.id=l.section_id JOIN content_package_versions v ON v.id=s.package_version_id "
                + "WHERE v.package_id=? AND a.asset_type='PASSAGE'", String.class, created)).containsExactly("A. Gardens help.\nB. They cost little.");
        List<UUID> versions = jdbc.queryForList("SELECT sq.question_version_id FROM section_questions sq JOIN content_sections s ON s.id=sq.section_id "
                + "JOIN content_package_versions v ON v.id=s.package_version_id WHERE v.package_id=? ORDER BY sq.sort_order", UUID.class, created);
        assertThat(versions).hasSize(3);
        assertThat(jdbc.queryForList("SELECT q.status FROM questions q JOIN question_versions qv ON qv.question_id=q.id WHERE qv.id IN ("
                + versions.stream().map(id -> "'" + id + "'").reduce((a, b) -> a + "," + b).orElseThrow() + ")", String.class))
                .containsOnly("DRAFT");
        assertThat(jdbc.queryForList("SELECT knowledge_point_id FROM question_knowledge_points WHERE question_version_id=?", UUID.class, versions.get(0)))
                .containsExactly(point);
        assertThat(jdbc.queryForObject("SELECT answer_spec::text FROM question_versions WHERE id=?", String.class, versions.get(1)))
                .contains("\"correct\"").contains("\"B\"");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question_versions WHERE id=? AND explanation='Because.' AND difficulty='MEDIUM'",
                Integer.class, versions.get(0))).isEqualTo(1);
    }

    private static QuestionPlan question(String stem, String correct) {
        return new QuestionPlan(stem, List.of(new OptionPlan("A", "First"), new OptionPlan("B", "Second"), new OptionPlan("C", "Third")),
                correct, "Because.", QuestionDifficulty.MEDIUM);
    }
}
