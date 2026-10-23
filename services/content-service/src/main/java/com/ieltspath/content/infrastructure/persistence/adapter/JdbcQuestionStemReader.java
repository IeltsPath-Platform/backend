package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionStemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JdbcQuestionStemReader implements QuestionStemReader {

    private final JdbcTemplate jdbc;

    public JdbcQuestionStemReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> stemsOf(UUID knowledgePointId) {
        return jdbc.queryForList("""
                SELECT v.stem
                FROM question_knowledge_points link
                JOIN question_versions v ON v.id = link.question_version_id
                WHERE link.knowledge_point_id = ? AND v.stem IS NOT NULL
                """, String.class, knowledgePointId);
    }
}
