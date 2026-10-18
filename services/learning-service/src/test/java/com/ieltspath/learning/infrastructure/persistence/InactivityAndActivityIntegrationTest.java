package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.InactivityFlagSync;
import com.ieltspath.learning.application.service.WorkItemSync;
import com.ieltspath.learning.application.usecase.GetActivityStatsUseCase;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** INACTIVE flags and activity statistics against a real database, with events placed at chosen times. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false", "learning.messaging.consumer-enabled=false",
        "learning.mastery.backfill-on-startup=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class InactivityAndActivityIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db").withPassword(UUID.randomUUID().toString());
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired InactivityFlagSync inactivity;
    @Autowired QuestionEventLog questionEvents;
    @Autowired GetActivityStatsUseCase stats;
    @Autowired WorkItemSync workItems;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void emptyTables() {
        jdbc.execute("TRUNCATE question_attempt_events, learner_flags, topic_progress, course_progress, work_items");
    }

    @Test
    void anInactiveFlagBecomesOneItemInTheExaminerQueue() {
        UUID stale = UUID.randomUUID();
        answeredAt(stale, Instant.now().minus(Duration.ofDays(10)));
        onCourse(stale, UUID.randomUUID(), false);
        inactivity.run();

        workItems.sync();
        workItems.sync();

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM work_items WHERE queue = 'EXAMINER' "
                + "AND kind = 'LEARNER_FLAG' AND subject_user_id = ? AND detail ->> 'kind' = 'INACTIVE'",
                Integer.class, stale), "one EXAMINER item for the flag, however often the round runs");
    }

    @Test
    void aLearnerWhoStoppedWhileOnACourseGetsOneFlagAndTheirNextAnswerClosesIt() {
        UUID stale = UUID.randomUUID();
        UUID passedCourse = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        Instant now = Instant.now();
        answeredAt(stale, now.minus(Duration.ofDays(10)));
        onCourse(stale, UUID.randomUUID(), false);
        answeredAt(passedCourse, now.minus(Duration.ofDays(10)));
        onCourse(passedCourse, UUID.randomUUID(), true);
        answeredAt(fresh, now.minus(Duration.ofHours(1)));
        onCourse(fresh, UUID.randomUUID(), false);

        assertEquals(1, inactivity.run(), "only the stale learner on an open course");
        assertEquals(1, openFlags(stale));
        assertEquals(0, openFlags(passedCourse));
        assertEquals(0, openFlags(fresh));

        assertEquals(0, inactivity.run(), "a second run does not open the same flag again");
        assertEquals(1, openFlags(stale));

        questionEvents.append(List.of(QuestionAttemptEvent.of(stale, UUID.randomUUID(), List.of(), true, 2000L, null,
                QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), true)));

        assertEquals(0, openFlags(stale), "answering again closes the flag");
        assertEquals("DISMISSED", jdbc.queryForObject("SELECT status FROM learner_flags WHERE user_id = ?",
                String.class, stale));
    }

    @Test
    void dailyWeeklyAndMonthlyActivityAndTheWeeklyRetentionOfEachCohort() {
        // Monday 2 March 2026 is the first week. X answers on the 2nd, 3rd and 10th; Y on the 3rd; Z on the 10th.
        LocalDate base = LocalDate.of(2026, 3, 2);
        UUID x = UUID.randomUUID();
        UUID y = UUID.randomUUID();
        UUID z = UUID.randomUUID();
        answeredAt(x, at(base));
        answeredAt(x, at(base.plusDays(1)));
        answeredAt(x, at(base.plusDays(8)));
        answeredAt(y, at(base.plusDays(1)));
        answeredAt(z, at(base.plusDays(8)));

        GetActivityStatsUseCase.Result result = stats.execute(base, base.plusDays(8));

        assertEquals(9, result.daily().size(), "both ends included");
        GetActivityStatsUseCase.Day first = result.daily().get(0);
        assertEquals(1, first.dau());
        GetActivityStatsUseCase.Day second = result.daily().get(1);
        assertEquals(2, second.dau());
        GetActivityStatsUseCase.Day tenth = result.daily().get(8);
        assertEquals(2, tenth.dau(), "X and Z answered on the 10th");
        assertEquals(2, tenth.wau(), "the week before the 10th: X and Z");
        assertEquals(3, tenth.mau(), "the 30 days before the 10th: X, Y and Z");

        assertEquals(2, result.retention().size(), () -> "cohorts: " + result.retention());
        GetActivityStatsUseCase.Cohort firstWeek = result.retention().get(0);
        assertEquals(2, firstWeek.size());
        assertEquals(0.5, firstWeek.week1(), 1e-9, "X answered in the second week");
        assertEquals(0.0, firstWeek.week2(), 1e-9);
        GetActivityStatsUseCase.Cohort secondWeek = result.retention().get(1);
        assertEquals(1, secondWeek.size());
        assertEquals(0.0, secondWeek.week1(), 1e-9);
    }

    @Test
    void aRangeIsRefusedWhenItEndsBeforeItStartsOrCoversMoreThan180Days() {
        LocalDate from = LocalDate.of(2026, 3, 2);

        LocalDate beforeStart = from.minusDays(1);
        LocalLengthError backwards = assertThrowsRange(() -> stats.execute(from, beforeStart));
        assertEquals("INVALID_RANGE", backwards.code());

        LocalLengthError tooLong = assertThrowsRange(() -> stats.execute(from, from.plusDays(180)));
        assertEquals("RANGE_TOO_LONG", tooLong.code());
        assertEquals(400, tooLong.status());
    }

    private record LocalLengthError(String code, int status) {}

    private static LocalLengthError assertThrowsRange(org.junit.jupiter.api.function.Executable call) {
        LearningRequestException error = assertThrows(LearningRequestException.class, call);
        return new LocalLengthError(error.getCode(), error.getStatus());
    }

    private int openFlags(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM learner_flags WHERE user_id = ? AND kind = 'INACTIVE' "
                + "AND status = 'OPEN'", Integer.class, user);
    }

    private void onCourse(UUID user, UUID course, boolean passed) {
        jdbc.update("INSERT INTO topic_progress (user_id, topic_id, sequence_order, course_id) VALUES (?, ?, 1, ?)",
                user, UUID.randomUUID(), course);
        if (passed) jdbc.update("INSERT INTO course_progress (user_id, course_id, passed_at) VALUES (?, ?, now())",
                user, course);
    }

    private void answeredAt(UUID user, Instant when) {
        jdbc.update("INSERT INTO question_attempt_events (id, user_id, question_version_id, correct, source, "
                + "source_reference_id, first_submission, occurred_at) VALUES (?, ?, ?, true, 'lesson_exercise', ?, "
                + "true, ?)", UUID.randomUUID(), user, UUID.randomUUID(), UUID.randomUUID(), Timestamp.from(when));
    }

    private static Instant at(LocalDate day) {
        return day.atTime(10, 0).toInstant(ZoneOffset.UTC);
    }
}
