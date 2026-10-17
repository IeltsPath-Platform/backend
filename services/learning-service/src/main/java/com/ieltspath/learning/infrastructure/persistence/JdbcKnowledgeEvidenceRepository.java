package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
                result_version, question_version_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            statement.setObject(9, item.questionVersionId());
        });
    }

    @Override
    public Map<UUID, List<EvidencePoint>> findEvidencePoints(UUID userId, Collection<UUID> knowledgePointIds,
                                                             int perPoint) {
        if (knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, List<EvidencePoint>> found = new HashMap<>();
        jdbc.query("""
                SELECT kp_id, correct, source, created_at, question_version_id FROM (
                    SELECT kp_id, correct, source, created_at, question_version_id, ordinal,
                    row_number() OVER (PARTITION BY kp_id ORDER BY ordinal DESC) AS recency
                    FROM kp_evidence WHERE user_id = ? AND kp_id = ANY (?)
                ) newest WHERE recency <= ? ORDER BY kp_id, ordinal
                """, statement -> {
            statement.setObject(1, userId);
            statement.setArray(2, uuidArray(statement.getConnection(), knowledgePointIds));
            statement.setInt(3, perPoint);
        }, rows -> {
            found.computeIfAbsent(rows.getObject("kp_id", UUID.class), ignored -> new ArrayList<>())
                    .add(new EvidencePoint(rows.getBoolean("correct"), EvidenceSource.fromValue(rows.getString("source")),
                            rows.getTimestamp("created_at").toInstant(),
                            rows.getObject("question_version_id", UUID.class)));
        });
        return found;
    }

    @Override
    public Set<UUID> removeAssessmentEvidence(UUID userId, UUID attemptId) {
        return new HashSet<>(jdbc.queryForList("""
                DELETE FROM kp_evidence WHERE user_id = ? AND attempt_id = ? AND source = 'assessment'
                RETURNING kp_id
                """, UUID.class, userId, attemptId));
    }

    @Override
    public List<MasteryHistory> findMasteryHistories(UUID userId) {
        return histories(userId, null);
    }

    @Override
    public List<MasteryHistory> findMasteryHistories(UUID userId, Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) return List.of();
        return histories(userId, knowledgePointIds);
    }

    @Override
    public Map<UUID, Instant> lastEvidenceAt(UUID userId, Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, Instant> latest = new HashMap<>();
        jdbc.query("""
                SELECT kp_id, max(created_at) AS last_at FROM kp_evidence
                WHERE user_id = ? AND kp_id = ANY (?) GROUP BY kp_id
                """, statement -> {
            statement.setObject(1, userId);
            statement.setArray(2, uuidArray(statement.getConnection(), knowledgePointIds));
        }, rows -> {
            latest.put(rows.getObject("kp_id", UUID.class), rows.getTimestamp("last_at").toInstant());
        });
        return latest;
    }

    static java.sql.Array uuidArray(java.sql.Connection connection, Collection<UUID> ids) throws java.sql.SQLException {
        return connection.createArrayOf("uuid", ids.stream().map(UUID::toString).toArray());
    }

    private List<MasteryHistory> histories(UUID userId, Collection<UUID> knowledgePointIds) {
        boolean filtered = knowledgePointIds != null;
        return jdbc.query("""
                WITH history AS (
                    SELECT kp_id, correct, ordinal, count(*) OVER (PARTITION BY kp_id) AS evidence_count,
                    row_number() OVER (PARTITION BY kp_id ORDER BY ordinal DESC) AS recency
                    FROM kp_evidence WHERE user_id = ?
                )
                SELECT catalog.kp_id, catalog.topic_id, catalog.has_practice_set, catalog.skill,
                history.correct, history.ordinal, COALESCE(history.evidence_count, 0) AS evidence_count
                FROM knowledge_point_catalog catalog LEFT JOIN history
                ON history.kp_id = catalog.kp_id AND history.recency <= 5
                """ + (filtered ? "WHERE catalog.kp_id = ANY (?)\n" : "") + """
                ORDER BY catalog.topic_id, catalog.kp_id, history.ordinal
                """, statement -> {
            statement.setObject(1, userId);
            if (filtered) statement.setArray(2, uuidArray(statement.getConnection(), knowledgePointIds));
        }, (ResultSetExtractor<List<MasteryHistory>>) rows -> {
            Map<UUID, Row> histories = new LinkedHashMap<>();
            while (rows.next()) {
                UUID kpId = rows.getObject("kp_id", UUID.class);
                Row history = histories.get(kpId);
                if (history == null) {
                    history = new Row(kpId, rows.getObject("topic_id", UUID.class), rows.getBoolean("has_practice_set"),
                            rows.getString("skill") == null ? null : LearningSkill.valueOf(rows.getString("skill")),
                            new ArrayList<>(), rows.getLong("evidence_count"));
                    histories.put(kpId, history);
                }
                if (rows.getObject("ordinal") != null) history.correctness().add(rows.getBoolean("correct"));
            }
            return histories.values().stream().map(row -> new MasteryHistory(row.kpId(), row.topicId(),
                    row.hasPracticeSet(), row.correctness(), row.evidenceCount(), row.skill())).toList();
        });
    }

    private record Row(UUID kpId, UUID topicId, boolean hasPracticeSet, LearningSkill skill,
                       List<Boolean> correctness, long evidenceCount) {}
}
