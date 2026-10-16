package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcQuestionEventLog implements QuestionEventLog {
    private final JdbcTemplate jdbc;

    public JdbcQuestionEventLog(NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = namedJdbc.getJdbcTemplate();
    }

    @Override
    public void append(List<QuestionAttemptEvent> events) {
        if (events.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO question_attempt_events (id, user_id, question_version_id, knowledge_point_ids, correct, score,
                    max_score, duration_ms, error_code, source, source_reference_id, attempt_id, first_submission)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, source, source_reference_id, question_version_id) DO NOTHING
                """, events, events.size(), (statement, event) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, event.userId());
            statement.setObject(3, event.questionVersionId());
            statement.setArray(4, statement.getConnection().createArrayOf("uuid",
                    event.knowledgePointIds().stream().map(UUID::toString).toArray()));
            if (event.correct() == null) statement.setNull(5, Types.BOOLEAN); else statement.setBoolean(5, event.correct());
            statement.setObject(6, event.score(), Types.NUMERIC);
            statement.setObject(7, event.maxScore(), Types.NUMERIC);
            statement.setObject(8, event.durationMs(), Types.BIGINT);
            statement.setObject(9, event.errorCode(), Types.VARCHAR);
            statement.setString(10, event.source().value());
            statement.setObject(11, event.sourceReferenceId());
            statement.setObject(12, event.attemptId(), Types.OTHER);
            statement.setBoolean(13, event.firstSubmission());
        });
    }

    @Override
    public void removeAssessmentEvents(UUID userId, UUID attemptId) {
        jdbc.update("DELETE FROM question_attempt_events WHERE user_id = ? AND attempt_id = ? AND source = 'assessment'",
                userId, attemptId);
    }
}
