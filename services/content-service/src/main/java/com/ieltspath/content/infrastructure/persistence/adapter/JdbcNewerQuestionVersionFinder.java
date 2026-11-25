package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.NewerQuestionVersionFinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcNewerQuestionVersionFinder implements NewerQuestionVersionFinder {

    private final JdbcTemplate jdbc;

    public JdbcNewerQuestionVersionFinder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> newerPublishedVersion(UUID questionVersionId) {
        return jdbc.query("""
                SELECT newer.id FROM question_versions current
                JOIN question_versions newer ON newer.question_id = current.question_id
                     AND newer.status = 'PUBLISHED' AND newer.version_number > current.version_number
                WHERE current.id = ?
                ORDER BY newer.version_number DESC LIMIT 1
                """, rs -> rs.next() ? Optional.of(rs.getObject(1, UUID.class)) : Optional.<UUID>empty(), questionVersionId);
    }
}
