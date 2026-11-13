package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.vo.RubricVersionRef;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcRubricRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class WritingRubricLowBandMigrationTest {
    private static final List<Double> LOW_BANDS = List.of(0.0, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcRubricRepository rubrics;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        rubrics = new JdbcRubricRepository(jdbc);
    }

    @Test
    void versionThreeIsTheOnlyActiveRubricOfEachTaskAndTheOlderVersionsAreRetired() {
        assertEquals(List.of(new RubricVersionRef("IELTS_WRITING_TASK_1", "TASK_1", 1, "RETIRED"),
                new RubricVersionRef("IELTS_WRITING_TASK_1", "TASK_1", 2, "RETIRED"),
                new RubricVersionRef("IELTS_WRITING_TASK_1", "TASK_1", 3, "ACTIVE")),
                rubrics.findVersions("IELTS_WRITING_TASK_1"));
        assertEquals(List.of(new RubricVersionRef("IELTS_WRITING_TASK_2", "TASK_2", 1, "RETIRED"),
                new RubricVersionRef("IELTS_WRITING_TASK_2", "TASK_2", 2, "RETIRED"),
                new RubricVersionRef("IELTS_WRITING_TASK_2", "TASK_2", 3, "ACTIVE")),
                rubrics.findVersions("IELTS_WRITING_TASK_2"));
    }

    @Test
    void everyActiveCriterionDescribesBandsZeroToNineWithoutHalfBandsBelowOne() {
        for (String task : List.of("TASK_1", "TASK_2")) {
            var rubric = rubrics.findActive(task).get(0);
            assertEquals(4, rubric.criteria().size(), task);
            for (var criterion : rubric.criteria()) {
                List<Double> bands = criterion.bandDescriptors().stream().map(item -> item.band()).sorted().toList();
                List<Double> expected = new java.util.ArrayList<>(LOW_BANDS);
                for (int step = 0; step <= 10; step++) expected.add(4.0 + step * 0.5);
                assertEquals(expected, bands, task + " " + criterion.code());
                assertTrue(criterion.bandDescriptors().stream().noneMatch(item -> item.descriptor().isBlank()));
                assertNotNull(criterion.knowledgePointId(), task + " " + criterion.code() + " keeps its knowledge point");
            }
        }
    }

    @Test
    void bandsFourToNineAreCopiedUnchangedFromVersionOne() {
        Integer differing = jdbc.queryForObject("""
                SELECT count(*) FROM rubric_band_descriptors old_d
                JOIN rubric_criteria old_c ON old_c.id = old_d.criterion_id
                JOIN rubrics old_r ON old_r.id = old_c.rubric_id AND old_r.rubric_version = 1
                JOIN rubrics new_r ON new_r.code = old_r.code AND new_r.rubric_version = 2
                JOIN rubric_criteria new_c ON new_c.rubric_id = new_r.id AND new_c.code = old_c.code
                LEFT JOIN rubric_band_descriptors new_d ON new_d.criterion_id = new_c.id AND new_d.band = old_d.band
                WHERE old_r.code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2')
                  AND new_d.descriptor IS DISTINCT FROM old_d.descriptor
                """, Map.of(), Integer.class);
        assertEquals(0, differing);
    }

    @Test
    void everyHalfBandNamesTheWholeBandsOnEachSide() {
        Integer wrong = jdbc.queryForObject("""
                SELECT count(*) FROM rubric_band_descriptors d
                JOIN rubric_criteria c ON c.id = d.criterion_id
                JOIN rubrics r ON r.id = c.rubric_id AND r.rubric_version = 3
                WHERE r.code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2')
                  AND d.band % 1 = 0.5
                  AND d.descriptor <> 'Meets every point of band ' || floor(d.band) || ' and some, but not all, points of band '
                                      || (floor(d.band) + 1) || '.'
                """, Map.of(), Integer.class);
        assertEquals(0, wrong);
    }

    @Test
    void examplesOfVersionTwoAreCopiedToVersionThree() {
        Integer examples = jdbc.queryForObject("""
                SELECT count(*) FROM rubric_examples e JOIN rubrics r ON r.id = e.rubric_id
                WHERE r.code IN ('IELTS_WRITING_TASK_1', 'IELTS_WRITING_TASK_2') AND r.rubric_version = 3
                """, Map.of(), Integer.class);
        assertEquals(2, examples);
    }
}
