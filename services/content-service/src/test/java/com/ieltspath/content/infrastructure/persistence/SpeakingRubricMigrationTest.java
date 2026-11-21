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
    void speakingHasExactlyOneActiveRubricAndEveryEarlierVersionIsRetired() {
        List<RubricVersionRef> versions = new JdbcRubricRepository(jdbc).findVersions("IELTS_SPEAKING");

        assertEquals(List.of(new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 1, "RETIRED"),
                new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 2, "RETIRED"),
                new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 3, "RETIRED"),
                new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 4, "RETIRED"),
                new RubricVersionRef("IELTS_SPEAKING", "SPEAKING", 5, "ACTIVE")), versions);
    }

    @Test
    void theCoherenceVersionChangesOnlyTheFluencyCriterionOfTheVersionBefore() {
        Integer otherCriteriaChanged = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT c.code, c.name, c.description, d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 5 AND c.code <> 'FC'
                    EXCEPT
                    SELECT c.code, c.name, c.description, d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 4 AND c.code <> 'FC') changed
                """, Map.of(), Integer.class);
        Integer fluencyBandsChanged = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 5 AND c.code = 'FC'
                    EXCEPT
                    SELECT d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 4 AND c.code = 'FC') changed
                """, Map.of(), Integer.class);
        Boolean samePrompt = jdbc.queryForObject("""
                SELECT (SELECT system_prompt FROM rubrics WHERE code = 'IELTS_SPEAKING' AND rubric_version = 5)
                     = (SELECT system_prompt FROM rubrics WHERE code = 'IELTS_SPEAKING' AND rubric_version = 4)
                """, Map.of(), Boolean.class);

        assertEquals(0, otherCriteriaChanged);
        assertEquals(4, fluencyBandsChanged, "only whole bands 4 to 7 of FC are rewritten");
        assertTrue(samePrompt);
    }

    @Test
    void sentencesThatWanderAwayFromTheQuestionCapFluencyAndCoherence() {
        var fluency = new JdbcRubricRepository(jdbc).findActive("SPEAKING").get(0).criteria().stream()
                .filter(item -> item.code().equals("FC")).findFirst().orElseThrow();
        String bandFour = fluency.bandDescriptors().stream()
                .filter(item -> item.band() == 4.0).findFirst().orElseThrow().descriptor();

        assertTrue(fluency.description().contains("three or more sentences that have nothing to do with the question or topic limit FC to 4.5"));
        assertTrue(fluency.description().contains("list each place once only"));
        assertTrue(bandFour.contains("three or more sentences have nothing to do with the question or topic"), bandFour);
    }

    @Test
    void theStricterVersionKeepsPronunciationAndTheLowestWholeBandsOfThePreviousVersion() {
        Integer pronunciationDifferences = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 4 AND c.code = 'PR'
                    EXCEPT
                    SELECT d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 3 AND c.code = 'PR') changed
                """, Map.of(), Integer.class);
        Integer lowBandDifferences = jdbc.queryForObject("""
                SELECT count(*) FROM (
                    SELECT c.code, d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 4 AND d.band IN (0, 1, 2, 3)
                    EXCEPT
                    SELECT c.code, d.band, d.descriptor FROM rubric_band_descriptors d
                    JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                    WHERE r.code = 'IELTS_SPEAKING' AND r.rubric_version = 3 AND d.band IN (0, 1, 2, 3)) changed
                """, Map.of(), Integer.class);

        assertEquals(0, pronunciationDifferences);
        assertEquals(0, lowBandDifferences);
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

    @Test
    void aHalfBandIsOnlyGivenWhenTheLowerBandIsSecureInEveryPart() {
        String descriptor = jdbc.queryForObject("""
                SELECT d.descriptor FROM rubric_band_descriptors d
                JOIN rubric_criteria c ON c.id = d.criterion_id JOIN rubrics r ON r.id = c.rubric_id
                WHERE r.task = 'SPEAKING' AND r.status = 'ACTIVE' AND c.code = 'GRA' AND d.band = 5.5
                """, Map.of(), String.class);

        assertTrue(descriptor.contains("met securely in every part"), descriptor);
        assertTrue(descriptor.contains("give band 5 or lower"), descriptor);
    }
}
