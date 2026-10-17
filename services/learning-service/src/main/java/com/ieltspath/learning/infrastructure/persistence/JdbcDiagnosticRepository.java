package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcDiagnosticRepository implements DiagnosticRepository {
    private static final String SESSION_COLUMNS = """
            id, user_id, root_review_id, root_kp_id, status, conclusion_kp_ids, created_at, concluded_at""";
    private static final String PROBE_COLUMNS = """
            id, session_id, kp_id, parent_kp_id, depth, package_id, package_version_id, status, correct, total,
            passed, request_id, answered_at""";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDiagnosticRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertSession(DiagnosticSession session) {
        jdbc.update("""
                INSERT INTO diagnostic_sessions (id, user_id, root_review_id, root_kp_id, status)
                VALUES (:id, :userId, :reviewId, :kpId, :status)
                """, Map.of("id", session.id(), "userId", session.userId(), "reviewId", session.rootReviewId(),
                "kpId", session.rootKnowledgePointId(), "status", session.status().name()));
    }

    @Override
    public void insertProbe(DiagnosticProbe probe) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", probe.id());
        params.put("sessionId", probe.sessionId());
        params.put("kpId", probe.knowledgePointId());
        params.put("parentKpId", probe.parentKnowledgePointId());
        params.put("depth", probe.depth());
        params.put("packageId", probe.packageId());
        params.put("versionId", probe.packageVersionId());
        params.put("status", probe.status().name());
        jdbc.update("""
                INSERT INTO diagnostic_probes (id, session_id, kp_id, parent_kp_id, depth, package_id,
                                               package_version_id, status)
                VALUES (:id, :sessionId, :kpId, :parentKpId, :depth, :packageId, :versionId, :status)
                """, params);
    }

    @Override
    public Optional<DiagnosticSession> findSession(UUID userId, UUID sessionId) {
        return jdbc.query("SELECT " + SESSION_COLUMNS + " FROM diagnostic_sessions WHERE id = :id AND user_id = :userId",
                Map.of("id", sessionId, "userId", userId), session()).stream().findFirst();
    }

    @Override
    public Optional<DiagnosticSession> findByRootReview(UUID reviewId) {
        return jdbc.query("SELECT " + SESSION_COLUMNS + " FROM diagnostic_sessions WHERE root_review_id = :id",
                Map.of("id", reviewId), session()).stream().findFirst();
    }

    @Override
    public List<DiagnosticProbe> findProbes(UUID sessionId) {
        return jdbc.query("SELECT " + PROBE_COLUMNS + " FROM diagnostic_probes WHERE session_id = :id "
                + "ORDER BY created_at, depth, kp_id", Map.of("id", sessionId), probe());
    }

    @Override
    public Optional<DiagnosticProbe> findProbe(UUID sessionId, UUID probeId) {
        return jdbc.query("SELECT " + PROBE_COLUMNS + " FROM diagnostic_probes WHERE id = :id AND session_id = :sessionId",
                Map.of("id", probeId, "sessionId", sessionId), probe()).stream().findFirst();
    }

    @Override
    public void answerProbe(UUID probeId, int correct, int total, boolean passed, UUID requestId) {
        int updated = jdbc.update("""
                UPDATE diagnostic_probes SET status = 'ANSWERED', correct = :correct, total = :total, passed = :passed,
                request_id = :requestId, answered_at = clock_timestamp()
                WHERE id = :id AND status = 'OPEN'
                """, Map.of("id", probeId, "correct", correct, "total", total, "passed", passed,
                "requestId", requestId));
        if (updated != 1) throw new IllegalStateException("Probe changed while it was answered");
    }

    @Override
    public void replaceProbeSet(UUID probeId, UUID packageId, UUID packageVersionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", probeId);
        params.put("packageId", packageId);
        params.put("versionId", packageVersionId);
        params.put("status", packageId == null ? "NO_SET" : "OPEN");
        jdbc.update("""
                UPDATE diagnostic_probes SET package_id = :packageId, package_version_id = :versionId, status = :status
                WHERE id = :id AND status = 'OPEN'
                """, params);
    }

    @Override
    public void conclude(UUID sessionId, Collection<UUID> knowledgePointIds) {
        jdbc.update("""
                UPDATE diagnostic_sessions SET status = 'CONCLUDED', conclusion_kp_ids = CAST(:kpIds AS uuid[]),
                concluded_at = clock_timestamp()
                WHERE id = :id AND status = 'PROBING'
                """, Map.of("id", sessionId, "kpIds", uuidArray(knowledgePointIds)));
    }

    @Override
    public Set<UUID> probingRootKnowledgePoints(UUID userId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT root_kp_id FROM diagnostic_sessions WHERE user_id = :userId AND status = 'PROBING'
                """, Map.of("userId", userId), UUID.class));
    }

    @Override
    public List<UUID> pendingWaitingOn(UUID reviewId) {
        return jdbc.queryForList("""
                SELECT w.id FROM review_items w
                WHERE w.status = 'PENDING'
                  AND w.id = ANY (SELECT unnest(waiting_on_review_ids) FROM review_items WHERE id = :id)
                ORDER BY w.created_at, w.id
                """, Map.of("id", reviewId), UUID.class);
    }

    @Override
    public void waitOn(UUID reviewId, Collection<UUID> reviewIds) {
        jdbc.update("UPDATE review_items SET waiting_on_review_ids = CAST(:ids AS uuid[]) WHERE id = :id",
                Map.of("id", reviewId, "ids", uuidArray(reviewIds)));
    }

    @Override
    public void recordContentGap(UUID knowledgePointId) {
        jdbc.update("""
                INSERT INTO content_gap_signals (kp_id, occurrences, first_detected_at, last_detected_at)
                VALUES (:kpId, 1, clock_timestamp(), clock_timestamp())
                ON CONFLICT (kp_id) DO UPDATE
                SET occurrences = content_gap_signals.occurrences + 1, last_detected_at = clock_timestamp()
                """, Map.of("kpId", knowledgePointId));
    }

    private static String uuidArray(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }

    private static RowMapper<DiagnosticSession> session() {
        return (row, index) -> new DiagnosticSession(row.getObject("id", UUID.class),
                row.getObject("user_id", UUID.class), row.getObject("root_review_id", UUID.class),
                row.getObject("root_kp_id", UUID.class), DiagnosticSession.Status.valueOf(row.getString("status")),
                SqlArrays.uuids(row.getArray("conclusion_kp_ids")), instant(row, "created_at"),
                instant(row, "concluded_at"));
    }

    private static RowMapper<DiagnosticProbe> probe() {
        return (row, index) -> new DiagnosticProbe(row.getObject("id", UUID.class),
                row.getObject("session_id", UUID.class), row.getObject("kp_id", UUID.class),
                row.getObject("parent_kp_id", UUID.class), row.getInt("depth"),
                row.getObject("package_id", UUID.class), row.getObject("package_version_id", UUID.class),
                DiagnosticProbe.Status.valueOf(row.getString("status")), integer(row, "correct"),
                integer(row, "total"), bool(row, "passed"), row.getObject("request_id", UUID.class),
                instant(row, "answered_at"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Integer integer(ResultSet row, String column) throws SQLException {
        int value = row.getInt(column);
        return row.wasNull() ? null : value;
    }

    private static Boolean bool(ResultSet row, String column) throws SQLException {
        boolean value = row.getBoolean(column);
        return row.wasNull() ? null : value;
    }
}
