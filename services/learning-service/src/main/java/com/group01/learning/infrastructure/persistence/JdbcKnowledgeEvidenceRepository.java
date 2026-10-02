package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.vo.KnowledgeEvidence;
import com.group01.learning.domain.vo.MasteryHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcKnowledgeEvidenceRepository implements KnowledgeEvidenceRepository {
    private final JdbcTemplate jdbc;

    public JdbcKnowledgeEvidenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(UUID userId, List<KnowledgeEvidence> evidence) {
        if (evidence.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, attempt_id,
                result_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, source, source_reference_id) DO NOTHING
                """, evidence, evidence.size(), (statement, item) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, item.knowledgePointId());
            statement.setBoolean(4, item.correct());
            statement.setString(5, item.source().value());
            statement.setObject(6, item.sourceReferenceId());
            statement.setObject(7, item.attemptId());
            statement.setObject(8, item.resultVersion());
        });
    }

    @Override
    public void removeAssessmentEvidence(UUID userId, UUID attemptId) {
        jdbc.update("DELETE FROM kp_evidence WHERE user_id = ? AND attempt_id = ? AND source = 'assessment'",
                userId, attemptId);
    }

    @Override
    public List<MasteryHistory> findMasteryHistories(UUID userId) {
        return jdbc.query("""
                WITH history AS (
                    SELECT kp_id, correct, ordinal, count(*) OVER (PARTITION BY kp_id) AS evidence_count,
                    row_number() OVER (PARTITION BY kp_id ORDER BY ordinal DESC) AS recency
                    FROM kp_evidence WHERE user_id = ?
                )
                SELECT catalog.kp_id, catalog.topic_id, catalog.has_practice_set,
                history.correct, history.ordinal, COALESCE(history.evidence_count, 0) AS evidence_count
                FROM knowledge_point_catalog catalog LEFT JOIN history
                ON history.kp_id = catalog.kp_id AND history.recency <= 5
                ORDER BY catalog.topic_id, catalog.kp_id, history.ordinal
                """, (ResultSetExtractor<List<MasteryHistory>>) rows -> {
            Map<UUID, Row> histories = new LinkedHashMap<>();
            while (rows.next()) {
                UUID kpId = rows.getObject("kp_id", UUID.class);
                Row history = histories.get(kpId);
                if (history == null) {
                    history = new Row(kpId, rows.getObject("topic_id", UUID.class), rows.getBoolean("has_practice_set"),
                            new ArrayList<>(), rows.getLong("evidence_count"));
                    histories.put(kpId, history);
                }
                if (rows.getObject("ordinal") != null) history.correctness().add(rows.getBoolean("correct"));
            }
            return histories.values().stream().map(row -> new MasteryHistory(row.kpId(), row.topicId(),
                    row.hasPracticeSet(), row.correctness(), row.evidenceCount())).toList();
        }, userId);
    }

    private record Row(UUID kpId, UUID topicId, boolean hasPracticeSet, List<Boolean> correctness, long evidenceCount) {}
}
