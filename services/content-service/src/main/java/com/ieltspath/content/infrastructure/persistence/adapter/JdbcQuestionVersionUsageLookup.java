package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionVersionUsageLookup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JdbcQuestionVersionUsageLookup implements QuestionVersionUsageLookup {
    private final JdbcTemplate jdbcTemplate;

    public JdbcQuestionVersionUsageLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isUsedByPublishedContent(UUID questionVersionId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT (
                    SELECT count(*) FROM lesson_block_questions bq
                    JOIN lesson_blocks b ON b.id = bq.block_id
                    JOIN lessons l ON l.id = b.lesson_id
                    WHERE bq.question_version_id = ? AND l.status = 'PUBLISHED'
                ) + (
                    SELECT count(*) FROM section_questions sq
                    JOIN content_sections s ON s.id = sq.section_id
                    JOIN content_package_versions pv ON pv.id = s.package_version_id
                    WHERE sq.question_version_id = ? AND pv.status = 'PUBLISHED'
                )
                """, Integer.class, questionVersionId, questionVersionId);
        return count != null && count > 0;
    }
}
