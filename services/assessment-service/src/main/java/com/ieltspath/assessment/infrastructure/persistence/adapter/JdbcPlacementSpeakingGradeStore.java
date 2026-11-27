package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.AssessmentMessaging;
import com.ieltspath.events.assessment.SpeakingGradingCompletedV1;
import com.ieltspath.events.assessment.SpeakingGradingRequestedV1;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPlacementSpeakingGradeStore implements PlacementSpeakingGradeStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OutboxTraceContext traceContext;

    public JdbcPlacementSpeakingGradeStore(JdbcTemplate jdbc, ObjectMapper json, OutboxTraceContext traceContext) {
        this.jdbc = jdbc;
        this.json = json;
        this.traceContext = traceContext;
    }

    @Override
    public List<RecordingItem> recordings(UUID attemptId, UUID userId) {
        return jdbc.query("""
                SELECT DISTINCT ON (ai.id) ai.id AS item_id,
                       COALESCE((ase.section_snapshot->>'partNo')::integer, ase.sort_order + 1) AS part_no,
                       ai.sort_order + 1 AS question_no,
                       COALESCE(ls.prompt_snapshot->>'stem', ai.question_snapshot->>'stem', ai.question_snapshot->>'prompt', '') AS question,
                       ls.audio_reference AS object_key
                FROM attempt_sections ase
                JOIN attempt_items ai ON ai.attempt_section_id = ase.id
                JOIN learner_submissions ls ON ls.attempt_item_id = ai.id
                JOIN assessment_attempts aa ON aa.id = ase.attempt_id
                WHERE aa.id = ? AND aa.user_id = ? AND aa.attempt_type = 'PLACEMENT'
                  AND upper(ase.section_snapshot->>'skill') = 'SPEAKING'
                  AND ls.user_id = aa.user_id AND ls.skill = 'SPEAKING' AND ls.status = 'SUBMITTED'
                  AND ls.audio_reference IS NOT NULL AND ls.text_payload IS NULL
                ORDER BY ai.id, ls.submitted_at DESC, ls.id DESC
                """, (rs, row) -> new RecordingItem(rs.getObject("item_id", UUID.class), rs.getInt("part_no"),
                rs.getInt("question_no"), rs.getString("question"), rs.getString("object_key")), attemptId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(SpeakingGradingRequestedV1 event, Instant deadline) {
        try {
            String payload = json.writeValueAsString(event);
            jdbc.update("""
                    INSERT INTO placement_speaking_grades(attempt_id,user_id,status,deadline_at)
                    VALUES (?,?,'PENDING',?) ON CONFLICT (attempt_id) DO NOTHING
                    """, event.data().attemptId(), event.data().userId(), Timestamp.from(deadline));
            jdbc.update("""
                    INSERT INTO outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at,
                                              correlation_id,trace_context)
                    VALUES (?,'PlacementSpeakingGrade',?,?,CAST(? AS JSONB),?,?,?)
                    ON CONFLICT (id) DO NOTHING
                    """, event.eventId(), event.data().attemptId().toString(), event.eventType(), payload,
                    Timestamp.from(event.occurredAt()), Correlation.current(), traceContext.capture());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Yêu cầu chấm Speaking không thể được ghi nhận.", exception);
        }
    }

    @Override
    @Transactional
    public boolean complete(SpeakingGradingCompletedV1 event) {
        int inserted = jdbc.update("""
                INSERT INTO consumed_speaking_grading_events(event_id,attempt_id) VALUES (?,?)
                ON CONFLICT DO NOTHING
                """, event.eventId(), event.data().attemptId());
        if (inserted == 0) return false;
        var data = event.data();
        String feedback;
        try { feedback = json.writeValueAsString(data); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Kết quả chấm Speaking không hợp lệ.", exception); }
        return jdbc.update("""
                UPDATE placement_speaking_grades
                SET status=?,band=?,feedback=CAST(? AS JSONB),rubric_code=?,rubric_version=?,failure_code=?,
                    completed_at=?,updated_at=CURRENT_TIMESTAMP
                WHERE attempt_id=? AND user_id=? AND status='PENDING'
                """, data.status(), data.band(), feedback, data.rubricCode(), data.rubricVersion(), data.failureCode(),
                Timestamp.from(event.occurredAt()), data.attemptId(), data.userId()) == 1;
    }

    @Override
    public Optional<Grade> find(UUID attemptId) {
        List<Grade> found = jdbc.query("""
                SELECT status,band,feedback::text,rubric_code,rubric_version,failure_code
                FROM placement_speaking_grades WHERE attempt_id=?
                """, (rs, row) -> new Grade(rs.getString("status"),
                rs.getObject("band", java.math.BigDecimal.class) == null ? null : rs.getBigDecimal("band").doubleValue(),
                rs.getString("feedback"), rs.getString("rubric_code"), (Integer) rs.getObject("rubric_version"),
                rs.getString("failure_code")), attemptId);
        return found.stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> failExpired(Instant now, int limit) {
        return jdbc.query("""
                WITH expired AS (
                    SELECT attempt_id FROM placement_speaking_grades
                    WHERE status='PENDING' AND deadline_at <= ?
                    ORDER BY deadline_at LIMIT ? FOR UPDATE SKIP LOCKED
                )
                UPDATE placement_speaking_grades g SET status='FAILED',failure_code='TIMEOUT',
                    completed_at=?,updated_at=CURRENT_TIMESTAMP FROM expired e
                WHERE g.attempt_id=e.attempt_id RETURNING g.attempt_id
                """, (rs, row) -> rs.getObject(1, UUID.class), Timestamp.from(now), limit, Timestamp.from(now));
    }
}
