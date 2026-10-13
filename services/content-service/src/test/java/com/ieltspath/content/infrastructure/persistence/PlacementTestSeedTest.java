package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PlacementTestSeedTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcLearningContentReader reader;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        reader = new JdbcLearningContentReader(jdbc);
    }

    @Test
    void onePublishedPlacementPackageCoversAllFourSkills() {
        var packages = reader.placementTestPackages();
        assertThat(packages).singleElement().satisfies(pkg -> assertThat(pkg.code()).isEqualTo("PLACEMENT-4SKILLS"));
        var payload = reader.publishedPackageVersion(packages.getFirst().packageVersionId()).orElseThrow();
        assertThat(payload.packageType()).isEqualTo(PackageType.PLACEMENT_TEST);
        Set<Skill> skills = payload.sections().stream().map(section -> section.skill()).collect(Collectors.toSet());
        assertThat(skills).containsExactlyInAnyOrder(Skill.LISTENING, Skill.READING, Skill.WRITING, Skill.SPEAKING);
        // Same parts and order as the real test.
        assertThat(payload.sections()).extracting(section -> section.title()).containsExactly(
                "Listening Part 1: joining a swimming course", "Listening Part 2: a visit to a nature reserve",
                "Listening Part 3: a project on canteen food waste", "Listening Part 4: the early history of the bicycle",
                "Reading Passage 1: bees in the city", "Reading Passage 2: the return of the night train",
                "Reading Passage 3: the case for handwriting",
                "Writing Task 1: museum visitors", "Writing Task 2: trees or parking",
                "Speaking Part 1: introduction and interview", "Speaking Part 2: a place you like",
                "Speaking Part 3: discussion");
        assertThat(payload.sections().get(7).passage()).startsWith("Museum | 2019");
    }

    @Test
    void listeningAndReadingHaveFortyQuestionsEachWithOneAudioOrPassagePerPart() {
        var payload = reader.publishedPackageVersion(reader.placementTestPackages().getFirst().packageVersionId())
                .orElseThrow();
        Map<Skill, Integer> questions = payload.sections().stream().collect(Collectors.groupingBy(
                section -> section.skill(), Collectors.summingInt(section -> section.items().size())));
        assertThat(questions).containsEntry(Skill.LISTENING, 40).containsEntry(Skill.READING, 40)
                .containsEntry(Skill.WRITING, 2).containsEntry(Skill.SPEAKING, 8);
        payload.sections().stream().filter(section -> section.skill() == Skill.LISTENING)
                .forEach(section -> assertThat(section.audio().mediaReference()).startsWith("listening/placement/part"));
        payload.sections().stream().filter(section -> section.skill() == Skill.READING)
                .forEach(section -> assertThat(section.passage()).isNotBlank());
    }

    @Test
    void placementQuestionsHaveTheirOwnPurposeAndNoOtherOwner() {
        var version = reader.placementTestPackages().getFirst().packageVersionId();
        assertThat(reader.questionsWithWrongPurpose(version, QuestionPurpose.PLACEMENT)).isEmpty();
        assertThat(reader.questionsUsedElsewhere(version)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM questions WHERE purpose = 'PLACEMENT'", Map.of(), Integer.class))
                .isEqualTo(101);
    }

    @Test
    void courseTestsAndMockTestsAreNotListedAsPlacementPackages() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_packages WHERE package_type = 'PLACEMENT_TEST'",
                Map.of(), Integer.class)).isEqualTo(1);
    }
}
