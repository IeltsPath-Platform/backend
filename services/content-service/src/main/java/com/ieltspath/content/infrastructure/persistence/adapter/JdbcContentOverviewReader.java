package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.ContentOverviewReader;
import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.result.ContentOverviewResult.CoverageCell;
import com.ieltspath.content.application.result.ContentOverviewResult.KnowledgePointCounts;
import com.ieltspath.content.application.result.ContentOverviewResult.QuestionLabel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcContentOverviewReader implements ContentOverviewReader {
    private static final List<String> LESSON_STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final List<String> PUBLICATION_STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");
    /** A draft question version nobody touched for this many days is reported as stale. */
    static final int STALE_DRAFT_AFTER_DAYS = 14;
    private static final int LABEL_LENGTH = 90;

    private final JdbcTemplate jdbcTemplate;

    public JdbcContentOverviewReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ContentOverviewResult read() {
        Map<String, Long> byStatus = grouped("SELECT status, count(*) FROM knowledge_points GROUP BY status");
        Map<String, Long> bySkill = grouped("""
                SELECT COALESCE(skill, 'NONE'), count(*) FROM knowledge_points WHERE status = 'ACTIVE' GROUP BY 1
                """);
        Map<String, Long> byCefr = grouped("""
                SELECT COALESCE(cefr_level, 'NONE'), count(*) FROM knowledge_points WHERE status = 'ACTIVE' GROUP BY 1
                """);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        return new ContentOverviewResult(
                new KnowledgePointCounts(total, byStatus, bySkill, byCefr),
                count("""
                        SELECT count(*) FROM knowledge_points kp
                        WHERE kp.status = 'ACTIVE' AND NOT EXISTS (
                            SELECT 1 FROM question_knowledge_points qkp
                            JOIN question_versions qv ON qv.id = qkp.question_version_id
                            WHERE qkp.knowledge_point_id = kp.id AND qv.status = 'PUBLISHED')
                        """),
                count("SELECT count(*) FROM question_versions WHERE status = 'DRAFT'"),
                withEveryStatus(LESSON_STATUSES, "SELECT status, count(*) FROM lessons GROUP BY status"),
                coverage(),
                withEveryStatus(PUBLICATION_STATUSES, "SELECT status, count(*) FROM question_versions GROUP BY status"),
                withEveryStatus(PUBLICATION_STATUSES, "SELECT status, count(*) FROM content_packages GROUP BY status"),
                jdbcTemplate.queryForObject("""
                        SELECT count(*) FROM question_versions
                        WHERE status = 'DRAFT' AND updated_at < now() - make_interval(days => ?)
                        """, Long.class, STALE_DRAFT_AFTER_DAYS),
                STALE_DRAFT_AFTER_DAYS);
    }

    /**
     * Active points by skill and CEFR level, with how many have no published question and how many exactly one. A point
     * counts once per question version, however many times a version links to it.
     */
    private List<CoverageCell> coverage() {
        return jdbcTemplate.query("""
                SELECT COALESCE(kp.skill, 'NONE') AS skill,
                       COALESCE(kp.cefr_level, 'NONE') AS cefr,
                       count(*) AS total,
                       count(*) FILTER (WHERE COALESCE(q.published, 0) = 0) AS without_question,
                       count(*) FILTER (WHERE q.published = 1) AS one_question
                FROM knowledge_points kp
                LEFT JOIN (
                    SELECT qkp.knowledge_point_id, count(DISTINCT qkp.question_version_id) AS published
                    FROM question_knowledge_points qkp
                    JOIN question_versions qv ON qv.id = qkp.question_version_id
                    WHERE qv.status = 'PUBLISHED'
                    GROUP BY qkp.knowledge_point_id) q ON q.knowledge_point_id = kp.id
                WHERE kp.status = 'ACTIVE'
                GROUP BY 1, 2
                ORDER BY 1, 2
                """, (resultSet, row) -> new CoverageCell(
                resultSet.getString("skill"), resultSet.getString("cefr"), resultSet.getLong("total"),
                resultSet.getLong("without_question"), resultSet.getLong("one_question")));
    }

    @Override
    public List<QuestionLabel> questionLabels(Collection<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) return List.of();
        String placeholders = String.join(", ", questionVersionIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("""
                SELECT qv.id, qv.question_id, q.skill, q.question_type,
                       left(regexp_replace(trim(qv.stem), '\\s+', ' ', 'g'), %d) AS label
                FROM question_versions qv
                JOIN questions q ON q.id = qv.question_id
                WHERE qv.id IN (%s)
                """.formatted(LABEL_LENGTH, placeholders),
                (resultSet, row) -> new QuestionLabel(
                        resultSet.getObject("id", UUID.class), resultSet.getObject("question_id", UUID.class),
                        resultSet.getString("label"), resultSet.getString("skill"), resultSet.getString("question_type")),
                questionVersionIds.toArray());
    }

    /** The counts by status with every status listed, so a status nothing has yet reads as zero. */
    private Map<String, Long> withEveryStatus(List<String> statuses, String sql) {
        Map<String, Long> counts = new LinkedHashMap<>();
        statuses.forEach(status -> counts.put(status, 0L));
        counts.putAll(grouped(sql));
        return counts;
    }

    private Map<String, Long> grouped(String sql) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, resultSet -> {
            counts.put(resultSet.getString(1), resultSet.getLong(2));
        });
        return counts;
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
