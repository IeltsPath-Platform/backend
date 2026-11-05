package com.ieltspath.content.infrastructure.persistence;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeded courses form a ladder on whole bands, and each course can be learned from start to finish: every
 * prerequisite is taught before the point that needs it, every point is taught and practisable, and every chặng and
 * course has a final test.
 */
@Testcontainers(disabledWithoutDocker = true)
class CourseLadderSeedTest {
    @Container private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static NamedParameterJdbcTemplate jdbc;

    /** A published practice set whose questions are not used by a lesson or a test, with at least three of them. */
    private static final String ELIGIBLE_SET_FOR_POINT = """
            EXISTS (SELECT 1 FROM content_packages p
                    WHERE p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED'
                      AND (SELECT count(*) FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                           WHERE s.package_version_id = p.current_published_version_id) >= 3
                      AND EXISTS (SELECT 1 FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                                  JOIN question_knowledge_points qk ON qk.question_version_id = sq.question_version_id
                                  WHERE s.package_version_id = p.current_published_version_id
                                    AND qk.knowledge_point_id = kp.id))
            """;

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
    void coursesSitOnWholeBandsFromFoundationToEight() {
        var courses = jdbc.queryForList("SELECT code, band_level, status FROM courses ORDER BY band_level", Map.of());
        assertThat(courses).extracting(row -> row.get("code"))
                .containsExactly("IELTS_FOUNDATION", "IELTS_5_0", "IELTS_6_0", "IELTS_7_0", "IELTS_8_0");
        assertThat(courses).extracting(row -> ((BigDecimal) row.get("band_level")).stripTrailingZeros().toPlainString())
                .containsExactly("4", "5", "6", "7", "8");
        assertThat(courses).extracting(row -> row.get("status")).containsOnly("ACTIVE");
    }

    @Test
    void everyPrerequisiteIsTaughtByALowerCourseOrEarlierInTheSameCourse() {
        List<Map<String, Object>> broken = jdbc.queryForList("""
                WITH placed AS (
                  SELECT kp.id, kp.code, t.course_id, c.band_level, t.sort_order AS topic_order, t.id AS topic_id,
                         (SELECT min(l.sort_order) FROM lesson_knowledge_points lk JOIN lessons l ON l.id = lk.lesson_id
                          WHERE lk.knowledge_point_id = kp.id AND l.topic_id = t.id) AS lesson_order
                  FROM knowledge_points kp JOIN topics t ON t.id = kp.topic_id LEFT JOIN courses c ON c.id = t.course_id)
                SELECT d.code AS dependent, p.code AS prerequisite
                FROM knowledge_point_prerequisites link
                JOIN placed d ON d.id = link.knowledge_point_id
                JOIN placed p ON p.id = link.prerequisite_id
                WHERE d.course_id IS NOT NULL
                  AND (p.course_id IS NULL
                       OR p.band_level > d.band_level
                       OR (p.course_id = d.course_id AND p.topic_order > d.topic_order)
                       OR (p.topic_id = d.topic_id AND p.lesson_order > d.lesson_order))
                """, Map.of());
        assertThat(broken).isEmpty();
    }

    @Test
    void prerequisitesHaveNoCycle() {
        Integer cycles = jdbc.queryForObject("""
                WITH RECURSIVE walk(current_id, path, cyclic) AS (
                  SELECT prerequisite_id, ARRAY[knowledge_point_id, prerequisite_id], knowledge_point_id = prerequisite_id
                  FROM knowledge_point_prerequisites
                  UNION ALL
                  SELECT l.prerequisite_id, w.path || l.prerequisite_id, l.prerequisite_id = ANY(w.path)
                  FROM walk w JOIN knowledge_point_prerequisites l ON l.knowledge_point_id = w.current_id
                  WHERE NOT w.cyclic AND array_length(w.path, 1) < 16)
                SELECT count(*) FROM walk WHERE cyclic
                """, Map.of(), Integer.class);
        assertThat(cycles).isZero();
    }

    @Test
    void everyPointOfACourseIsTaughtAndPractisable() {
        // Error traps hang off a parent point, and writing and speaking criteria are judged when an essay or a recording is graded.
        List<String> missing = jdbc.queryForList("""
                SELECT kp.code FROM knowledge_points kp JOIN topics t ON t.id = kp.topic_id
                WHERE t.course_id IS NOT NULL AND t.status = 'ACTIVE' AND kp.status = 'ACTIVE'
                  AND kp.code NOT LIKE 'TRAP\\_%'
                  AND NOT EXISTS (SELECT 1 FROM writing_criterion_knowledge_points wc WHERE wc.knowledge_point_id = kp.id)
                  AND NOT EXISTS (SELECT 1 FROM speaking_criterion_knowledge_points sc WHERE sc.knowledge_point_id = kp.id)
                  AND (NOT EXISTS (SELECT 1 FROM lesson_knowledge_points lk JOIN lessons l ON l.id = lk.lesson_id
                                   WHERE lk.knowledge_point_id = kp.id AND l.status = 'PUBLISHED')
                       OR NOT\s""" + ELIGIBLE_SET_FOR_POINT + ") ORDER BY kp.code", Map.of(), String.class);
        assertThat(missing).isEmpty();
    }

    @Test
    void everyLessonPracticeSetPractisesAPointItsLessonTeaches() {
        List<String> stray = jdbc.queryForList("""
                SELECT p.code FROM content_packages p
                WHERE p.package_type = 'PRACTICE_SET' AND p.lesson_id IS NOT NULL AND p.status = 'PUBLISHED'
                  AND NOT EXISTS (SELECT 1 FROM content_sections s
                                  JOIN section_questions sq ON sq.section_id = s.id
                                  JOIN question_knowledge_points qk ON qk.question_version_id = sq.question_version_id
                                  JOIN lesson_knowledge_points lk ON lk.knowledge_point_id = qk.knowledge_point_id
                                  WHERE s.package_version_id = p.current_published_version_id
                                    AND lk.lesson_id = p.lesson_id)
                ORDER BY p.code
                """, Map.of(), String.class);
        assertThat(stray).isEmpty();
    }

    @Test
    void everyChangHasALessonAndAFinalTestAndEveryCourseHasACourseTest() {
        List<String> untested = jdbc.queryForList("""
                SELECT t.code FROM topics t
                WHERE t.course_id IS NOT NULL AND t.status = 'ACTIVE'
                  AND (NOT EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id = t.id AND l.status = 'PUBLISHED')
                       OR NOT EXISTS (SELECT 1 FROM content_packages p WHERE p.topic_id = t.id
                                      AND p.package_type = 'TOPIC_TEST' AND p.status = 'PUBLISHED'))
                ORDER BY t.code
                """, Map.of(), String.class);
        assertThat(untested).isEmpty();
        List<String> withoutCourseTest = jdbc.queryForList("""
                SELECT c.code FROM courses c WHERE NOT EXISTS (SELECT 1 FROM content_packages p
                    WHERE p.course_id = c.id AND p.package_type = 'COURSE_TEST' AND p.status = 'PUBLISHED')
                """, Map.of(), String.class);
        assertThat(withoutCourseTest).isEmpty();
    }

    @Test
    void onlyFoundationIsFreeAndItSellsNothing() {
        assertThat(jdbc.queryForList("SELECT code FROM courses WHERE free ORDER BY band_level", Map.of(), String.class))
                .containsExactly("IELTS_FOUNDATION");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM course_skill_prices p JOIN courses c ON c.id = p.course_id WHERE c.free
                """, Map.of(), Integer.class)).isZero();
        var reader = new com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader(jdbc);
        assertThat(reader.freeCourseIds()).hasSize(1);
    }

    @Test
    void everyTaughtSkillOfFoundationAndFiveHasAtLeastSixLessons() {
        List<String> thin = jdbc.queryForList("""
                SELECT c.code || '/' || t.skill FROM lessons l JOIN topics t ON t.id = l.topic_id
                JOIN courses c ON c.id = t.course_id
                WHERE c.code IN ('IELTS_FOUNDATION', 'IELTS_5_0') AND l.status = 'PUBLISHED'
                GROUP BY c.code, t.skill HAVING count(*) < 6 ORDER BY 1
                """, Map.of(), String.class);
        assertThat(thin).isEmpty();
    }
}
