package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.aggregate.Rubric;
import com.ieltspath.content.domain.exception.RubricNotEditableException;
import com.ieltspath.content.application.usecase.ManageRubricsUseCase;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcRubricRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class RubricRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcRubricRepository rubrics;
    private static ManageRubricsUseCase manageRubrics;
    private static TransactionTemplate transactions;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        rubrics = new JdbcRubricRepository(jdbc);
        manageRubrics = new ManageRubricsUseCase(rubrics);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    @Test
    void migrationSeedsBothTasksWithPublicSourceAndCriteriaDescriptorsAndExamples() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rubrics WHERE code IN ('IELTS_WRITING_TASK_1','IELTS_WRITING_TASK_2') "
                + "AND status='ACTIVE'", java.util.Map.of(), Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rubric_criteria c JOIN rubrics r ON r.id=c.rubric_id "
                + "WHERE r.code IN ('IELTS_WRITING_TASK_1','IELTS_WRITING_TASK_2')", java.util.Map.of(), Integer.class))
                .isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rubric_band_descriptors d JOIN rubric_criteria c ON c.id=d.criterion_id "
                + "JOIN rubrics r ON r.id=c.rubric_id WHERE r.code IN ('IELTS_WRITING_TASK_1','IELTS_WRITING_TASK_2')",
                java.util.Map.of(), Integer.class))
                .isEqualTo(88);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rubric_examples e JOIN rubrics r ON r.id=e.rubric_id "
                + "WHERE r.code IN ('IELTS_WRITING_TASK_1','IELTS_WRITING_TASK_2')", java.util.Map.of(), Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM rubrics WHERE code IN ('IELTS_WRITING_TASK_1','IELTS_WRITING_TASK_2') "
                + "AND source_note LIKE '%ielts.org%'", java.util.Map.of(), Integer.class))
                .isEqualTo(2);
    }

    @Test
    void seededRubricsGiveEveryCriterionItsOwnKnowledgePointPerTask() {
        Rubric task1 = rubrics.findActive("TASK_1").getFirst();
        Rubric task2 = rubrics.findActive("TASK_2").getFirst();

        assertThat(task1.criteria()).extracting(Rubric.Criterion::knowledgePointId)
                .doesNotContainNull().doesNotHaveDuplicates();
        assertThat(task2.criteria()).extracting(Rubric.Criterion::knowledgePointId)
                .doesNotContainNull().doesNotHaveDuplicates();
        assertCriterionMapping(task1, Map.of(
                "TA", "DEMO_WRITING_CRITERION_TA",
                "CC", "DEMO_WRITING_CRITERION_CC",
                "LR", "DEMO_WRITING_CRITERION_LR",
                "GRA", "DEMO_WRITING_CRITERION_GRA"));
        assertCriterionMapping(task2, Map.of(
                "TR", "DEMO_WRITING_CRITERION_TR",
                "CC", "DEMO_WRITING_CRITERION_CC",
                "LR", "DEMO_WRITING_CRITERION_LR",
                "GRA", "DEMO_WRITING_CRITERION_GRA"));
        assertThat(criterion(task1, "CC").knowledgePointId())
                .isEqualTo(criterion(task2, "CC").knowledgePointId());
    }

    @Test
    void rubricCriteriaListTheirSubKnowledgePointsInOrder() {
        Rubric task1 = rubrics.findActive("TASK_1").getFirst();
        Rubric task2 = rubrics.findActive("TASK_2").getFirst();

        assertThat(criterion(task1, "CC").subKnowledgePoints()).extracting(Rubric.SubKnowledgePoint::code)
                .containsExactly("DEMO_WRITING_LINKING");
        assertThat(criterion(task2, "CC").subKnowledgePoints()).extracting(Rubric.SubKnowledgePoint::code)
                .containsExactly("DEMO_WRITING_TOPIC_SENTENCE", "DEMO_WRITING_LINKING");
        task1.criteria().stream().filter(value -> !value.code().equals("CC"))
                .forEach(value -> assertThat(value.subKnowledgePoints()).isEmpty());
        task2.criteria().stream().filter(value -> !value.code().equals("CC"))
                .forEach(value -> assertThat(value.subKnowledgePoints()).isEmpty());
        assertThat(java.util.stream.Stream.concat(task1.criteria().stream(), task2.criteria().stream())
                .flatMap(value -> value.subKnowledgePoints().stream()).toList())
                .allSatisfy(point -> {
                    assertThat(point.id()).isNotNull();
                    assertThat(point.name()).isNotBlank();
                    assertThat(point.description()).isNotBlank();
                });
    }

    @Test
    void subKnowledgePointsAreActiveWritingPointsOfActiveCourses() {
        assertThat(jdbc.getJdbcTemplate().queryForList("""
                SELECT count(*)
                FROM writing_criterion_sub_knowledge_points s
                JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
                JOIN topics t ON t.id = kp.topic_id
                JOIN courses c ON c.id = t.course_id
                WHERE kp.status = 'ACTIVE' AND kp.skill = 'WRITING'
                  AND t.status = 'ACTIVE' AND c.status = 'ACTIVE'
                """, Integer.class)).containsExactly(3);
    }

    @Test
    void rubricOmitsSubKnowledgePointsWhenTheirTopicBecomesInactive() {
        jdbc.getJdbcTemplate().update("""
                UPDATE topics SET status = 'INACTIVE'
                WHERE id = (SELECT topic_id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING')
                """);
        try {
            Rubric task1 = rubrics.findActive("TASK_1").getFirst();
            Rubric task2 = rubrics.findActive("TASK_2").getFirst();

            assertThat(criterion(task1, "CC").subKnowledgePoints()).isEmpty();
            assertThat(criterion(task2, "CC").subKnowledgePoints()).isEmpty();
        } finally {
            jdbc.getJdbcTemplate().update("""
                    UPDATE topics SET status = 'ACTIVE'
                    WHERE id = (SELECT topic_id FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING')
                    """);
        }
    }

    @Test
    void subKnowledgePointMappingRejectsDuplicatesInvalidOrderAndMissingCriterion() {
        assertThatThrownBy(() -> jdbc.getJdbcTemplate().update("""
                INSERT INTO writing_criterion_sub_knowledge_points (task, criterion_code, knowledge_point_id, sort_order)
                SELECT 'TASK_1', 'CC', id, 1 FROM knowledge_points WHERE code = 'DEMO_WRITING_LINKING'
                """ )).isInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> jdbc.getJdbcTemplate().update("""
                INSERT INTO writing_criterion_sub_knowledge_points (task, criterion_code, knowledge_point_id, sort_order)
                SELECT 'TASK_1', 'CC', id, 5 FROM knowledge_points WHERE code = 'DEMO_WRITING_TOPIC_SENTENCE'
                """ )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.getJdbcTemplate().update("""
                INSERT INTO writing_criterion_sub_knowledge_points (task, criterion_code, knowledge_point_id, sort_order)
                SELECT 'TASK_1', 'TR', id, 1 FROM knowledge_points WHERE code = 'DEMO_WRITING_TOPIC_SENTENCE'
                """ )).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void linkerMisuseIsLinkedToTheLinkingKnowledgePoint() {
        assertThat(jdbc.getJdbcTemplate().queryForObject("""
                SELECT e.knowledge_point_id = kp.id
                FROM error_types e CROSS JOIN knowledge_points kp
                WHERE e.code = 'LINKER_MISUSE' AND kp.code = 'DEMO_WRITING_LINKING'
                """, Boolean.class)).isTrue();
    }

    @BeforeEach
    void restoreSeedRubrics() {
        jdbc.getJdbcTemplate().update("DELETE FROM rubrics WHERE id NOT IN "
                + "('39a00000-0000-4000-8000-000000000001','39a00000-0000-4000-8000-000000000002')");
        jdbc.getJdbcTemplate().update("UPDATE rubrics SET status='ACTIVE' WHERE rubric_version=1");
    }

    @Test
    void draftsCanBeEditedOnceAndActivationRetiresTheOldVersionButKeepsItReadable() {
        transactions.execute(status -> rubrics.activate("IELTS_WRITING_TASK_2", 1));
        Rubric draft = draft("IELTS_WRITING_TASK_2", "TASK_2", 2, "Draft source note", "Initial descriptor");
        Rubric saved = transactions.execute(status -> rubrics.create(draft));
        assertThat(saved.status()).isEqualTo("DRAFT");

        Rubric edited = transactions.execute(status -> rubrics.updateDraft(draft.code(), draft.version(),
                draft(draft.code(), draft.task(), draft.version(), "Updated source note", "Updated descriptor")));
        assertThat(edited.sourceNote()).isEqualTo("Updated source note");
        assertThat(edited.criteria().getFirst().bandDescriptors().getFirst().descriptor()).isEqualTo("Updated descriptor");

        Rubric activated = transactions.execute(status -> rubrics.activate(draft.code(), draft.version()));
        assertThat(activated.status()).isEqualTo("ACTIVE");
        Rubric previous = manageRubrics.byVersion("IELTS_WRITING_TASK_2", 1);
        assertThat(previous.status()).isEqualTo("RETIRED");
        assertThat(previous.criteria()).hasSize(4);
        assertThat(previous.criteria()).allSatisfy(criterion -> assertThat(criterion.knowledgePointId()).isNotNull());
        assertThat(rubrics.findActive("TASK_2")).extracting(Rubric::code).containsExactly(draft.code());
        assertThatThrownBy(() -> transactions.execute(status -> rubrics.updateDraft(draft.code(), draft.version(), draft)))
                .isInstanceOf(RubricNotEditableException.class);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO rubrics (code, task, rubric_version, status, source_note) "
                        + "VALUES ('SECOND_ACTIVE_TASK_2', 'TASK_2', 1, 'ACTIVE', 'seed')", java.util.Map.of()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void activationRejectsWrongTaskCriteriaAndIncompleteHalfBandCoverage() {
        Rubric wrongCriteria = new Rubric("BAD_TASK_1_CRITERIA", "TASK_1", 1, "Source", "DRAFT",
                List.of(criterion("TR", 1, "Full"), criterion("CC", 2, "Full"),
                        criterion("LR", 3, "Full"), criterion("GRA", 4, "Full")), List.of());
        transactions.execute(status -> rubrics.create(wrongCriteria));
        assertThatThrownBy(() -> transactions.execute(status -> rubrics.activate(wrongCriteria.code(), 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requires criteria");

        Rubric missingBand = draft("MISSING_HALF_BAND", "TASK_2", 1, "Source", "Full");
        Rubric.Criterion first = missingBand.criteria().getFirst();
        List<Rubric.Criterion> incomplete = new java.util.ArrayList<>(missingBand.criteria());
        incomplete.set(0, new Rubric.Criterion(first.code(), first.name(), first.description(), first.sortOrder(),
                first.bandDescriptors().stream().filter(value -> value.band() != 6.5).toList()));
        missingBand = new Rubric(missingBand.code(), missingBand.task(), missingBand.version(), missingBand.sourceNote(),
                "DRAFT", incomplete, List.of());
        Rubric incompleteDraft = missingBand;
        transactions.execute(status -> rubrics.create(incompleteDraft));
        assertThatThrownBy(() -> transactions.execute(status -> rubrics.activate(incompleteDraft.code(), 1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("one descriptor for every band");
    }

    private static Rubric draft(String code, String task, int version, String sourceNote, String descriptor) {
        List<Rubric.Criterion> criteria = List.of(
                criterion("TR", 1, descriptor), criterion("CC", 2, descriptor),
                criterion("LR", 3, descriptor), criterion("GRA", 4, descriptor));
        return new Rubric(code, task, version, sourceNote, "DRAFT", criteria, List.of());
    }

    private static Rubric.Criterion criterion(String code, int order, String descriptor) {
        List<Rubric.BandDescriptor> bands = java.util.stream.IntStream.rangeClosed(0, 10)
                .mapToObj(step -> new Rubric.BandDescriptor(4.0 + step * 0.5, descriptor)).toList();
        return new Rubric.Criterion(code, code, "Criterion description", order,
                bands);
    }

    private static void assertCriterionMapping(Rubric rubric, Map<String, String> expected) {
        assertThat(rubric.criteria()).hasSize(expected.size());
        expected.forEach((criterionCode, knowledgePointCode) -> {
            Rubric.Criterion criterion = criterion(rubric, criterionCode);
            assertThat(jdbc.getJdbcTemplate().queryForObject(
                    "SELECT code FROM knowledge_points WHERE id = ?", String.class, criterion.knowledgePointId()))
                    .isEqualTo(knowledgePointCode);
        });
    }

    private static Rubric.Criterion criterion(Rubric rubric, String code) {
        return rubric.criteria().stream().filter(criterion -> criterion.code().equals(code)).findFirst().orElseThrow();
    }
}
