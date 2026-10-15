package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.CourseStudyActivity;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class LearningProgressIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    static JdbcTemplate sql;
    static JdbcLearningProgressReader reader;
    static JdbcCourseStudyActivityRepository activity;

    @BeforeAll static void migrate() {
        var ds = new PGSimpleDataSource();
        ds.setURL(POSTGRES.getJdbcUrl()); ds.setUser(POSTGRES.getUsername()); ds.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        sql = new JdbcTemplate(ds);
        reader = new JdbcLearningProgressReader(new NamedParameterJdbcTemplate(ds));
        activity = new JdbcCourseStudyActivityRepository(sql);
    }

    @Test void legacyStudyKeepsUnknownDatesAndCountsOnlyOwnedCompletedLessons() {
        UUID user = UUID.randomUUID(), course = UUID.randomUUID(), topic = UUID.randomUUID(), lesson = UUID.randomUUID();
        sql.update("INSERT INTO topic_progress(user_id,topic_id,course_id,sequence_order) VALUES (?,?,?,1)",user,topic,course);
        sql.update("INSERT INTO lesson_progress(user_id,lesson_id,topic_id,lesson_sort_order,completed_at) VALUES (?,?,?,1,now())",user,lesson,topic);
        assertThat(reader.startedCourses(user)).singleElement().satisfies(a -> {
            assertThat(a.courseId()).isEqualTo(course);
            assertThat(a.startedAt()).isNull();
            assertThat(a.lastStudiedAt()).isNull();
        });
        assertThat(reader.completedLessons(user,List.of(lesson))).containsExactly(lesson);
        assertThat(reader.completedLessons(UUID.randomUUID(),List.of(lesson))).isEmpty();
        assertThat(activity.hasLegacyStudy(user,course)).isTrue();
        Instant first = Instant.parse("2026-10-15T00:00:00Z");
        activity.save(CourseStudyActivity.start(user,course,first,true));
        activity.save(activity.find(user,course).orElseThrow().studiedAt(first.plusSeconds(60)));
        assertThat(reader.startedCourses(user)).singleElement().satisfies(a -> {
            assertThat(a.startedAt()).isNull();
            assertThat(a.lastStudiedAt()).isEqualTo(first.plusSeconds(60));
        });
    }

    @Test void newActivityRetainsFirstStudyAndIsUniqueAcrossRetries() {
        UUID user = UUID.randomUUID(), course = UUID.randomUUID();
        Instant first = Instant.parse("2026-10-15T00:00:00Z");
        var started = CourseStudyActivity.start(user,course,first,false);
        activity.save(started);
        activity.save(started.studiedAt(first.plusSeconds(30)));
        assertThat(reader.startedCourses(user)).singleElement().satisfies(a -> {
            assertThat(a.startedAt()).isEqualTo(first);
            assertThat(a.lastStudiedAt()).isEqualTo(first.plusSeconds(30));
        });
        assertThat(reader.startedCourses(UUID.randomUUID())).isEmpty();
    }
}
