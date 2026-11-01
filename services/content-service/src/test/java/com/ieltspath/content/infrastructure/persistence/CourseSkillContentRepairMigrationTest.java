package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class CourseSkillContentRepairMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> FRESH_DATABASE = new PostgreSQLContainer<>("postgres:15-alpine");
    @Container
    private static final PostgreSQLContainer<?> UPGRADE_DATABASE = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate freshJdbc;
    private static JdbcTemplate upgradeJdbc;
    private static Map<String, String> questionVersionsBeforeUpgrade;

    @BeforeAll
    static void migrateDatabases() {
        DataSource freshSource = dataSource(FRESH_DATABASE);
        Flyway.configure().dataSource(freshSource).locations("classpath:db/migration").load().migrate();
        freshJdbc = new JdbcTemplate(freshSource);

        DataSource upgradeSource = dataSource(UPGRADE_DATABASE);
        Flyway.configure().dataSource(upgradeSource).locations("classpath:db/migration").target("88").load().migrate();
        upgradeJdbc = new JdbcTemplate(upgradeSource);
        questionVersionsBeforeUpgrade = questionVersionContents(upgradeJdbc);
        Flyway.configure().dataSource(upgradeSource).locations("classpath:db/migration").load().migrate();
    }

    @Test
    void freshDatabaseHasOneSkillPerCourseTopicLessonAndKeepsTreeCourseCoverage() {
        assertCleanAudit(freshJdbc);
        assertSkillCounts(freshJdbc);
        assertSplitPackages(freshJdbc);
    }

    @Test
    void upgradeFromVersionEightyEightKeepsPublishedQuestionVersionsAndReferences() {
        assertCleanAudit(upgradeJdbc);
        assertSkillCounts(upgradeJdbc);
        assertSplitPackages(upgradeJdbc);
        assertThat(questionVersionContents(upgradeJdbc)).isEqualTo(questionVersionsBeforeUpgrade);
    }

    @Test
    void publishedLessonCountsCoverEveryCourseSkillPair() {
        assertThat(freshJdbc.queryForList("""
                WITH course_skills AS (
                    SELECT c.id AS course_id, c.code AS course_code, c.band_level, skill.name AS skill
                    FROM courses c
                    CROSS JOIN (VALUES ('LISTENING'), ('READING'), ('WRITING'), ('SPEAKING')) AS skill(name)
                )
                SELECT cs.course_code || '/' || cs.skill || ':' || count(DISTINCT l.id) || '/' ||
                       greatest(count(DISTINCT l.id) - 3, 0)
                FROM course_skills cs
                LEFT JOIN topics t ON t.course_id = cs.course_id AND t.skill = cs.skill
                LEFT JOIN lessons l ON l.topic_id = t.id AND l.status = 'PUBLISHED'
                GROUP BY cs.course_id, cs.course_code, cs.band_level, cs.skill
                ORDER BY cs.band_level, cs.course_code, cs.skill
                """, String.class)).containsExactly(
                "IELTS_5_5/LISTENING:4/1", "IELTS_5_5/READING:11/8", "IELTS_5_5/SPEAKING:0/0", "IELTS_5_5/WRITING:7/4",
                "IELTS_6_0/LISTENING:0/0", "IELTS_6_0/READING:0/0", "IELTS_6_0/SPEAKING:0/0", "IELTS_6_0/WRITING:0/0",
                "IELTS_6_5/LISTENING:1/0", "IELTS_6_5/READING:1/0", "IELTS_6_5/SPEAKING:0/0", "IELTS_6_5/WRITING:1/0",
                "IELTS_7_0/LISTENING:0/0", "IELTS_7_0/READING:0/0", "IELTS_7_0/SPEAKING:0/0", "IELTS_7_0/WRITING:0/0");
    }

    private static DataSource dataSource(PostgreSQLContainer<?> postgres) {
        var source = new PGSimpleDataSource();
        source.setURL(postgres.getJdbcUrl());
        source.setUser(postgres.getUsername());
        source.setPassword(postgres.getPassword());
        return source;
    }

    private static void assertCleanAudit(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForList("""
                SELECT t.code FROM topics t JOIN courses c ON c.id = t.course_id
                WHERE t.skill IS NULL ORDER BY t.code
                """, String.class)).isEmpty();
        assertThat(jdbc.queryForList("""
                SELECT kp.code FROM knowledge_points kp
                JOIN topics t ON t.id = kp.topic_id JOIN courses c ON c.id = t.course_id
                WHERE kp.skill <> 'ALL' AND (t.skill IS NULL OR kp.skill <> t.skill)
                ORDER BY kp.code
                """, String.class)).isEmpty();
        assertThat(jdbc.queryForList("""
                WITH taught_skills AS (
                    SELECT l.id AS lesson_id, kp.skill FROM lessons l
                    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
                    JOIN lesson_knowledge_points lkp ON lkp.lesson_id = l.id
                    JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id WHERE kp.skill <> 'ALL'
                    UNION
                    SELECT l.id, kp.skill FROM lessons l
                    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
                    JOIN lesson_blocks b ON b.lesson_id = l.id AND b.block_type = 'TEXT'
                    JOIN lesson_block_knowledge_points bkp ON bkp.block_id = b.id
                    JOIN knowledge_points kp ON kp.id = bkp.knowledge_point_id WHERE kp.skill <> 'ALL'
                    UNION
                    SELECT l.id, q.skill FROM lessons l
                    JOIN topics t ON t.id = l.topic_id AND t.course_id IS NOT NULL
                    JOIN lesson_blocks b ON b.lesson_id = l.id AND b.block_type = 'EXERCISE'
                    JOIN lesson_block_questions bq ON bq.block_id = b.id
                    JOIN question_versions qv ON qv.id = bq.question_version_id
                    JOIN questions q ON q.id = qv.question_id WHERE q.skill <> 'ALL'
                ), lesson_facts AS (
                    SELECT c.code AS course_code, t.code AS topic_code, l.code AS lesson_code,
                           t.skill AS topic_skill, array_agg(DISTINCT ts.skill ORDER BY ts.skill) AS taught_skills,
                           count(DISTINCT ts.skill) AS skill_count
                    FROM lessons l JOIN topics t ON t.id = l.topic_id JOIN courses c ON c.id = t.course_id
                    LEFT JOIN taught_skills ts ON ts.lesson_id = l.id
                    WHERE l.status = 'PUBLISHED'
                    GROUP BY c.code, t.code, l.code, t.skill
                )
                SELECT course_code || '/' || topic_code || '/' || lesson_code
                FROM lesson_facts
                WHERE skill_count > 1 OR (skill_count > 0 AND (topic_skill IS NULL OR taught_skills[1] <> topic_skill))
                ORDER BY 1
                """, String.class)).isEmpty();
    }

    private static void assertSkillCounts(JdbcTemplate jdbc) {
        Map<String, Integer> counts = jdbc.query("""
                SELECT t.skill, count(DISTINCT l.id) AS lesson_count
                FROM courses c JOIN topics t ON t.course_id = c.id
                LEFT JOIN lessons l ON l.topic_id = t.id AND l.status = 'PUBLISHED'
                WHERE c.code = 'IELTS_5_5'
                GROUP BY t.skill
                """, rs -> {
            var result = new java.util.HashMap<String, Integer>();
            while (rs.next()) result.put(rs.getString("skill"), rs.getInt("lesson_count"));
            return result;
        });
        assertThat(counts).containsEntry("LISTENING", 4).containsEntry("READING", 11).containsEntry("WRITING", 7);
        assertThat(jdbc.queryForList("""
                SELECT t.skill || ':' || count(l.id)
                FROM topics t JOIN lessons l ON l.topic_id = t.id AND l.status = 'PUBLISHED'
                WHERE t.code IN ('TREES_READING', 'TREES_LISTENING', 'TREES_WRITING')
                GROUP BY t.skill ORDER BY t.skill
                """, String.class)).containsExactly("LISTENING:2", "READING:2", "WRITING:2");
    }

    private static void assertSplitPackages(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForList("""
                SELECT code FROM topics WHERE id IN (
                    '2a000000-0000-4000-8000-010000000001',
                    '2a000000-0000-4000-8000-010000000002',
                    '2a000000-0000-4000-8000-010000000003') ORDER BY code
                """, String.class)).containsExactly("TREES_LISTENING", "TREES_READING", "TREES_WRITING");
        assertThat(jdbc.queryForList("""
                SELECT code FROM content_packages WHERE code LIKE 'TR-TOPIC-TEST%' AND status = 'PUBLISHED'
                ORDER BY code
                """, String.class)).containsExactly("TR-TOPIC-TEST", "TR-TOPIC-TEST-LISTENING", "TR-TOPIC-TEST-WRITING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content_packages WHERE code = 'TR-PS-MIXED' AND status = 'ARCHIVED'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("""
                SELECT q.id::text || ':' || qv.status
                FROM content_packages p JOIN content_package_versions pv ON pv.package_id = p.id
                JOIN content_sections s ON s.package_version_id = pv.id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE p.code LIKE 'TR-TOPIC-TEST%' ORDER BY q.id
                """, String.class)).hasSize(7).allSatisfy(item -> assertThat(item).endsWith(":PUBLISHED"));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM lessons WHERE id IN (
                    '2a000000-0000-4000-8000-030000000003',
                    '2a000000-0000-4000-8000-030000000004',
                    '2a000000-0000-4000-8000-030000000005',
                    '2a000000-0000-4000-8000-030000000006') AND status = 'PUBLISHED'
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT lesson_id::text FROM lesson_resources WHERE id = '31000000-0000-4000-8000-000000000007'
                """, String.class)).isEqualTo("2a000000-0000-4000-8000-030000000003");
        assertThat(jdbc.queryForList("""
                SELECT lesson_id::text FROM lesson_resources WHERE id IN (
                    '31000000-0000-4000-8000-000000000004',
                    '31000000-0000-4000-8000-000000000005',
                    '31000000-0000-4000-8000-000000000006') ORDER BY id
                """, String.class)).containsOnly("2a000000-0000-4000-8000-030000000001");
    }

    private static Map<String, String> questionVersionContents(JdbcTemplate jdbc) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT qv.id::text, qv.stem, qv.options::text, qv.answer_spec::text, qv.explanation, qv.status
                FROM question_versions qv
                WHERE qv.id::text BETWEEN '2a000000-0000-4000-8000-070000000027'
                  AND '2a000000-0000-4000-8000-070000000033'
                ORDER BY qv.id
                """);
        var result = new java.util.LinkedHashMap<String, String>();
        for (Map<String, Object> row : rows) {
            result.put((String) row.get("id"), row.toString());
        }
        return result;
    }
}
