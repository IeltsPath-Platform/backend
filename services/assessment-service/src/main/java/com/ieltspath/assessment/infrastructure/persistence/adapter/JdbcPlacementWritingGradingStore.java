package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.PlacementWritingGradingStore;
import com.ieltspath.commonobservability.Correlation;
import com.ieltspath.commonobservability.OutboxTraceContext;
import com.ieltspath.events.assessment.PlacementWritingGradingCompletedV1;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcPlacementWritingGradingStore implements PlacementWritingGradingStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final OutboxTraceContext traceContext;

    public JdbcPlacementWritingGradingStore(JdbcTemplate jdbc, ObjectMapper json, OutboxTraceContext traceContext) {
        this.jdbc = jdbc;
        this.json = json;
        this.traceContext = traceContext;
    }

    @Override
    public List<Essay> queued(UUID attemptId, UUID userId) {
        return jdbc.query("""
                SELECT ai.id AS item_id, ai.question_version_id, ls.text_payload,
                       ai.question_snapshot::text AS question_snapshot, ai.answer_snapshot::text AS answer_snapshot,
                       (SELECT string_agg(k.knowledge_point_id::text, ',' ORDER BY k.weight DESC, k.knowledge_point_id)
                          FROM attempt_item_knowledge_points k WHERE k.attempt_item_id = ai.id) AS knowledge_points
                FROM grading_jobs gj
                JOIN learner_submissions ls ON ls.id = gj.submission_id
                JOIN attempt_items ai ON ai.id = ls.attempt_item_id
                JOIN attempt_sections ase ON ase.id = ai.attempt_section_id
                WHERE ase.attempt_id = ? AND gj.user_id = ? AND gj.grading_mode = 'AI' AND gj.status = 'QUEUED'
                ORDER BY ase.sort_order, ai.sort_order
                """, (rs, row) -> {
            String points = rs.getString("knowledge_points");
            return new Essay(rs.getObject("item_id", UUID.class), rs.getObject("question_version_id", UUID.class),
                    rs.getString("text_payload"), rs.getString("question_snapshot"), rs.getString("answer_snapshot"),
                    points == null ? List.of()
                            : java.util.Arrays.stream(points.split(",")).map(UUID::fromString).toList());
        }, attemptId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void delegate(PlacementWritingGradingRequestedV1 event, Instant now) {
        for (var item : event.data().items()) {
            jdbc.update("""
                    UPDATE grading_jobs SET status = 'PROCESSING', processing_started_at = ?
                    WHERE idempotency_key = ? AND user_id = ? AND grading_mode = 'AI' AND status = 'QUEUED'
                    """, Timestamp.from(now), key(item.attemptItemId()), event.data().userId());
        }
        try {
            jdbc.update("""
                    INSERT INTO outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at,
                                              correlation_id,trace_context)
                    VALUES (?,'PlacementWritingGrade',?,?,CAST(? AS JSONB),?,?,?)
                    ON CONFLICT (id) DO NOTHING
                    """, event.eventId(), event.data().attemptId().toString(), event.eventType(),
                    json.writeValueAsString(event), Timestamp.from(event.occurredAt()), Correlation.current(),
                    traceContext.capture());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Yêu cầu chấm Writing không thể được ghi nhận.", exception);
        }
    }

    /**
     * Applied once per event: a redelivered FAILED would otherwise put back a job the local grader has claimed since,
     * and grade it twice.
     */
    @Override
    @Transactional
    public void record(UUID eventId, PlacementWritingGradingCompletedV1.Data result, Instant now) {
        int first = jdbc.update("""
                INSERT INTO consumed_placement_writing_events(event_id, attempt_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, eventId, result.attemptId());
        if (first == 0) return;
        for (var item : result.items()) {
            if (PlacementWritingGradingCompletedV1.GRADED.equals(item.status())) {
                jdbc.update("""
                        UPDATE grading_jobs SET status = 'COMPLETED', llm_band = ?, llm_feedback = CAST(? AS JSONB),
                            prompt_version = ?, completed_at = ?, processing_started_at = NULL
                        WHERE idempotency_key = ? AND user_id = ? AND grading_mode = 'AI'
                          AND status IN ('QUEUED', 'PROCESSING')
                        """, item.band(), feedback(item), item.promptVersion(), Timestamp.from(now),
                        key(item.attemptItemId()), result.userId());
            } else {
                jdbc.update("""
                        UPDATE grading_jobs SET status = 'QUEUED', processing_started_at = NULL
                        WHERE idempotency_key = ? AND user_id = ? AND grading_mode = 'AI' AND status = 'PROCESSING'
                        """, key(item.attemptItemId()), result.userId());
            }
        }
    }

    private static String key(UUID attemptItemId) {
        return "gate-essay:" + attemptItemId + ":ai";
    }

    /** The same JSON the local grader stores, so every reader of an essay job keeps working. */
    private String feedback(PlacementWritingGradingCompletedV1.Item item) {
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("summary", item.summary());
        feedback.put("criteria", item.criteria().stream().map(criterion -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", criterion.code());
            row.put("band", criterion.band());
            row.put("comment", criterion.comment());
            return row;
        }).toList());
        feedback.put("focus", item.focus());
        try {
            return json.writeValueAsString(feedback);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Kết quả chấm Writing không hợp lệ.", exception);
        }
    }
}
