package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.infrastructure.persistence.adapter.JdbcErrorTypeRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class ErrorTypeSeedMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
    }

    @Test
    void seedsTheTwentyFiveDraftCodesAsActive() {
        assertThat(count("SELECT count(*) FROM error_types WHERE status = 'ACTIVE'")).isEqualTo(28);
    }

    @Test
    void everySeededErrorTypeIsStillADraftAwaitingReview() {
        assertThat(count("SELECT count(*) FROM error_types WHERE review_status = 'DRAFT'")).isEqualTo(28);
    }

    @Test
    void gradingCatalogAndQuestionCodesRequireHumanApproval() {
        jdbc.getJdbcTemplate().update("UPDATE error_types SET review_status = 'APPROVED' WHERE code = 'PARAPHRASE_TRAP'");
        var repository = new JdbcErrorTypeRepository(jdbc);

        assertThat(repository.findApprovedActive()).extracting(errorType -> errorType.getCode())
                .containsExactly("PARAPHRASE_TRAP");
        assertThat(repository.findActiveCodes(Set.of("PARAPHRASE_TRAP", "GRAMMAR_TENSE")))
                .containsExactly("PARAPHRASE_TRAP");
    }

    @AfterEach
    void restoreSeedReviewStatus() {
        jdbc.getJdbcTemplate().update("UPDATE error_types SET review_status='DRAFT' WHERE code='PARAPHRASE_TRAP'");
    }

    @Test
    void everyCodeFollowsTheNamingRuleAndHasAHint() {
        assertThat(count("SELECT count(*) FROM error_types WHERE code !~ '^[A-Z][A-Z0-9_]*$' "
                + "OR btrim(fix_hint) = ''")).isZero();
    }

    @Test
    void knowledgePointLinksResolveToSeededKnowledgePoints() {
        // Nine codes are not tied to any knowledge point (grammar, plural form, coherence, off-topic, passage mismatch).
        assertThat(count("SELECT count(*) FROM error_types WHERE knowledge_point_id IS NULL")).isEqualTo(9);
        assertThat(count("SELECT count(*) FROM error_types e JOIN knowledge_points kp ON kp.id = e.knowledge_point_id")).isEqualTo(19);
        assertThat(count("SELECT count(*) FROM error_types e JOIN knowledge_points kp ON kp.id = e.knowledge_point_id "
                + "WHERE e.code = 'PARAPHRASE_TRAP' AND kp.code = 'R65_PARAPHRASE'")).isEqualTo(1);
    }

    private static long count(String sql) {
        return jdbc.queryForObject(sql, Map.of(), Long.class);
    }
}
