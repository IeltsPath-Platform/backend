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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class SpeakingRubricMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new NamedParameterJdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void speakingHasExactlyOneActiveRubricAndTheStarterDraftIsRetired() {
        List<RubricVersionRef> versions = new JdbcRubricRepository(jdbc).findVersions("IELTS_SPEAKING");

        assertEquals(List.of(new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 1, "RETIRED"),
                new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 2, "ACTIVE")), versions);
    }

    @Test
    void theActiveRubricGivesTheGraderFourCriteriaWithEveryBandAndASystemPrompt() {
        var rubric = new JdbcRubricRepository(jdbc).findActive("SPEAKING").get(0);

        assertEquals(List.of("FC", "LR", "GRA", "PR"), rubric.criteria().stream().map(item -> item.code()).toList());
        for (var criterion : rubric.criteria()) {
            assertEquals(19, criterion.bandDescriptors().size(), criterion.code());
            assertTrue(criterion.bandDescriptors().stream().noneMatch(item -> item.descriptor().isBlank()), criterion.code());
        }
        assertTrue(rubric.systemPrompt().length() > 500 && rubric.systemPrompt().length() <= 4000);
    }

    @Test
    void halfBandsAreDescribedAsLyingBetweenTheTwoWholeBands() {
        String descriptor = jdbc.queryForObject("""
                SELECT d.descriptor FROM rubric_band_descriptors d
                JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE' AND c.code = 'FC' AND d.band = 6.5
                """, Map.of(), String.class);

        assertTrue(descriptor.startsWith("Between band 6 and band 7"), descriptor);
    }
}
