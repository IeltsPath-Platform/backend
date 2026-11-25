package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionUsageReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcQuestionUsageReader implements QuestionUsageReader {

    private final JdbcTemplate jdbc;

    public JdbcQuestionUsageReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Usage> usagesOf(UUID questionId) {
        return jdbc.query("""
                SELECT 'PACKAGE' AS kind, p.id AS target_id, p.code AS target_code, p.title AS target_title,
                       s.title AS location, v.version_number AS target_version_number, v.status AS target_status,
                       qv.id AS question_version_id, qv.version_number AS question_version_number
                FROM question_versions qv
                JOIN section_questions sq ON sq.question_version_id = qv.id
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE qv.question_id = ?
                UNION ALL
                SELECT 'LESSON', l.id, l.code, l.title, 'Khối ' || lb.sort_order, NULL, l.status,
                       qv.id, qv.version_number
                FROM question_versions qv
                JOIN lesson_block_questions lbq ON lbq.question_version_id = qv.id
                JOIN lesson_blocks lb ON lb.id = lbq.block_id
                JOIN lessons l ON l.id = lb.lesson_id
                WHERE qv.question_id = ?
                ORDER BY 1, 3, 6 DESC NULLS LAST, 5
                """, (rs, row) -> new Usage(Kind.valueOf(rs.getString("kind")), rs.getObject("target_id", UUID.class),
                rs.getString("target_code"), rs.getString("target_title"), rs.getString("location"),
                (Integer) rs.getObject("target_version_number"), rs.getString("target_status"),
                rs.getObject("question_version_id", UUID.class), rs.getInt("question_version_number")),
                questionId, questionId);
    }
}
