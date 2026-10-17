package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.QuestionEventsAppended;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcQuestionEventLog implements QuestionEventLog {
    private final JdbcTemplate jdbc;

    private final org.springframework.context.ApplicationEventPublisher publisher;

    public JdbcQuestionEventLog(NamedParameterJdbcTemplate namedJdbc,
                                org.springframework.context.ApplicationEventPublisher publisher) {
        this.jdbc = namedJdbc.getJdbcTemplate();
        this.publisher = publisher;
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
        // Learners whose events were just written: their open flags are closed before the commit, and they are checked
        // for anomalies once it commits.
        events.stream().map(QuestionAttemptEvent::userId).distinct()
                .forEach(user -> publisher.publishEvent(new QuestionEventsAppended(user)));
    }

    @Override
    public void removeAssessmentEvents(UUID userId, UUID attemptId) {
        jdbc.update("DELETE FROM question_attempt_events WHERE user_id = ? AND attempt_id = ? AND source = 'assessment'",
                userId, attemptId);
    }

    @Override
    public Map<UUID, EventStats> statsByKnowledgePoint(UUID userId, Collection<UUID> knowledgePointIds, Instant since) {
        if (knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, Long> medians = new HashMap<>();
        jdbc.query("""
                SELECT point.id AS kp_id, percentile_cont(0.5) WITHIN GROUP (ORDER BY event.duration_ms) AS median_ms
                FROM unnest(CAST(? AS uuid[])) AS point(id)
                JOIN question_attempt_events event ON event.user_id = ? AND point.id = ANY (event.knowledge_point_ids)
                    AND event.occurred_at >= ?
                WHERE event.duration_ms IS NOT NULL
                GROUP BY point.id
                """, statement -> {
            statement.setArray(1, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(), knowledgePointIds));
            statement.setObject(2, userId);
            statement.setTimestamp(3, Timestamp.from(since));
        }, rows -> {
            medians.put(rows.getObject("kp_id", UUID.class), Math.round(rows.getDouble("median_ms")));
        });
        Map<UUID, List<ErrorCount>> errors = new HashMap<>();
        jdbc.query("""
                SELECT kp_id, error_code, n FROM (
                    SELECT point.id AS kp_id, event.error_code, count(*) AS n,
                           row_number() OVER (PARTITION BY point.id ORDER BY count(*) DESC, event.error_code) AS rnk
                    FROM unnest(CAST(? AS uuid[])) AS point(id)
                    JOIN question_attempt_events event ON event.user_id = ? AND point.id = ANY (event.knowledge_point_ids)
                        AND event.occurred_at >= ?
                    WHERE event.error_code IS NOT NULL
                    GROUP BY point.id, event.error_code
                ) ranked WHERE rnk <= 3 ORDER BY kp_id, rnk
                """, statement -> {
            statement.setArray(1, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(), knowledgePointIds));
            statement.setObject(2, userId);
            statement.setTimestamp(3, Timestamp.from(since));
        }, rows -> {
            errors.computeIfAbsent(rows.getObject("kp_id", UUID.class), ignored -> new ArrayList<>())
                    .add(new ErrorCount(rows.getString("error_code"), rows.getInt("n")));
        });
        Map<UUID, EventStats> stats = new HashMap<>();
        for (UUID point : new HashSet<>(union(medians.keySet(), errors.keySet()))) {
            stats.put(point, new EventStats(medians.get(point), errors.getOrDefault(point, List.of())));
        }
        return stats;
    }

    @Override
    public Map<UUID, List<RecentEvent>> recentByKnowledgePoint(UUID userId, Collection<UUID> knowledgePointIds,
                                                               int perPoint) {
        if (knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, List<RecentEvent>> recent = new HashMap<>();
        jdbc.query("""
                SELECT point.id AS kp_id, event.question_version_id, event.correct, event.duration_ms, event.source,
                       event.error_code, event.occurred_at,
                       essay.revision_no, essay.overall_band - earlier.overall_band AS revision_delta
                FROM unnest(CAST(? AS uuid[])) AS point(id)
                CROSS JOIN LATERAL (
                    SELECT * FROM question_attempt_events candidate
                    WHERE candidate.user_id = ? AND point.id = ANY (candidate.knowledge_point_ids)
                    ORDER BY candidate.ordinal DESC LIMIT ?
                ) event
                LEFT JOIN lesson_writing_submissions essay
                       ON event.source = 'lesson_writing' AND essay.id = event.source_reference_id
                LEFT JOIN lesson_writing_submissions earlier ON earlier.id = essay.previous_submission_id
                ORDER BY point.id, event.ordinal DESC
                """, statement -> {
            statement.setArray(1, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(), knowledgePointIds));
            statement.setObject(2, userId);
            statement.setInt(3, perPoint);
        }, rows -> {
            long duration = rows.getLong("duration_ms");
            Long durationMs = rows.wasNull() ? null : duration;
            boolean correct = rows.getBoolean("correct");
            Boolean correctness = rows.wasNull() ? null : correct;
            recent.computeIfAbsent(rows.getObject("kp_id", UUID.class), ignored -> new ArrayList<>())
                    .add(new RecentEvent(rows.getObject("question_version_id", UUID.class), correctness, durationMs,
                            QuestionEventSource.fromValue(rows.getString("source")), rows.getString("error_code"),
                            rows.getTimestamp("occurred_at").toInstant(), rows.getObject("revision_no", Integer.class),
                            rows.getBigDecimal("revision_delta")));
        });
        return recent;
    }

    private static Set<UUID> union(Set<UUID> first, Set<UUID> second) {
        Set<UUID> all = new HashSet<>(first);
        all.addAll(second);
        return all;
    }
}
