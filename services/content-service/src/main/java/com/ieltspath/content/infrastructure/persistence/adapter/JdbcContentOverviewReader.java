package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.ContentOverviewReader;
import com.ieltspath.content.application.result.ContentOverviewResult;
import com.ieltspath.content.application.result.ContentOverviewResult.KnowledgePointCounts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JdbcContentOverviewReader implements ContentOverviewReader {
    private static final List<String> LESSON_STATUSES = List.of("DRAFT", "PUBLISHED", "ARCHIVED");

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

        Map<String, Long> lessons = new LinkedHashMap<>();
        LESSON_STATUSES.forEach(status -> lessons.put(status, 0L));
        lessons.putAll(grouped("SELECT status, count(*) FROM lessons GROUP BY status"));

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
                lessons);
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
