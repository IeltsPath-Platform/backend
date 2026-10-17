package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class QuestionEventLogIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;
    private static JdbcQuestionEventLog log;

    @BeforeAll
    static void migrate() {
        var source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("11").load().migrate();
        jdbc = new NamedParameterJdbcTemplate(source);
        log = new JdbcQuestionEventLog(jdbc, event -> { });
    }

    @Test
    void writesOneRowPerQuestionWithItsDurationKnowledgePointAndFirstSubmissionFlag() {
        UUID user = UUID.randomUUID();
        UUID request = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        log.append(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                .stream().map(question -> QuestionAttemptEvent.of(user, question, List.of(kp), true, 5000L, null,
                        QuestionEventSource.PRACTICE_SET, request, true)).toList());

        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user", Map.of("user", user)))
                .isEqualTo(5);
        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user AND duration_ms = 5000 "
                + "AND first_submission AND :kp = ANY(knowledge_point_ids)", Map.of("user", user, "kp", kp))).isEqualTo(5);
    }

    @Test
    void aReplayedSourceReferenceAddsNothing() {
        UUID user = UUID.randomUUID();
        QuestionAttemptEvent event = QuestionAttemptEvent.of(user, UUID.randomUUID(), List.of(), false, null, "SPELLING",
                QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true);
        log.append(List.of(event));
        log.append(List.of(event));

        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user", Map.of("user", user)))
                .isEqualTo(1);
    }

    @Test
    void aLaterSubmissionOfTheSameQuestionIsLoggedAsARepeat() {
        UUID user = UUID.randomUUID();
        UUID question = UUID.randomUUID();
        log.append(List.of(QuestionAttemptEvent.of(user, question, List.of(), true, null, null,
                QuestionEventSource.PRACTICE_SET, UUID.randomUUID(), true)));
        log.append(List.of(QuestionAttemptEvent.of(user, question, List.of(), false, null, null,
                QuestionEventSource.PRACTICE_SET, UUID.randomUUID(), false)));

        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user AND NOT first_submission",
                Map.of("user", user))).isEqualTo(1);
    }

    @Test
    void removesOnlyTheAssessmentEventsOfTheRegradedAttempt() {
        UUID user = UUID.randomUUID();
        UUID attempt = UUID.randomUUID();
        UUID otherAttempt = UUID.randomUUID();
        log.append(List.of(
                new QuestionAttemptEvent(user, UUID.randomUUID(), List.of(), true, BigDecimal.ONE, BigDecimal.ONE, null,
                        null, QuestionEventSource.ASSESSMENT, UUID.randomUUID(), attempt, true),
                new QuestionAttemptEvent(user, UUID.randomUUID(), List.of(), true, BigDecimal.ONE, BigDecimal.ONE, null,
                        null, QuestionEventSource.ASSESSMENT, UUID.randomUUID(), otherAttempt, true),
                QuestionAttemptEvent.of(user, UUID.randomUUID(), List.of(), true, null, null,
                        QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true)));

        log.removeAssessmentEvents(user, attempt);

        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user", Map.of("user", user)))
                .isEqualTo(2);
        assertThat(count("SELECT count(*) FROM question_attempt_events WHERE user_id = :user AND attempt_id = :attempt",
                Map.of("user", user, "attempt", attempt))).isZero();
    }

    private static long count(String sql, Map<String, Object> params) {
        return jdbc.queryForObject(sql, params, Long.class);
    }
}
