package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.PlacementAssignment;
import com.ieltspath.learning.domain.repository.PlacementAssignmentRepository;
import com.ieltspath.learning.domain.vo.PlacementPool;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPlacementAssignmentRepository implements PlacementAssignmentRepository {
    private static final String COLUMNS = "id,user_id,package_id,package_version_id,pool,assigned_at,completed_at,released_at,attempt_id";
    private static final RowMapper<PlacementAssignment> ROW = (row, i) -> PlacementAssignment.restore(
            row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
            row.getObject("package_id", UUID.class), row.getObject("package_version_id", UUID.class),
            PlacementPool.valueOf(row.getString("pool")), row.getTimestamp("assigned_at").toInstant(),
            row.getTimestamp("completed_at") == null ? null : row.getTimestamp("completed_at").toInstant(),
            row.getTimestamp("released_at") == null ? null : row.getTimestamp("released_at").toInstant(),
            row.getObject("attempt_id", UUID.class));

    private final NamedParameterJdbcTemplate jdbc;
    public JdbcPlacementAssignmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public int countSince(UUID userId, Instant since) {
        return jdbc.queryForObject("SELECT count(*) FROM placement_assignments WHERE user_id=:userId " +
                        "AND assigned_at > :since AND released_at IS NULL",
                Map.of("userId", userId, "since", Timestamp.from(since)), Integer.class);
    }

    @Override public Optional<Instant> oldestSince(UUID userId, Instant since) {
        return jdbc.query("SELECT min(assigned_at) AS oldest FROM placement_assignments WHERE user_id=:userId " +
                        "AND assigned_at > :since AND released_at IS NULL",
                Map.of("userId", userId, "since", Timestamp.from(since)), (ResultSetExtractor<Optional<Instant>>) rs -> {
                    if (!rs.next() || rs.getTimestamp("oldest") == null) return Optional.empty();
                    return Optional.of(rs.getTimestamp("oldest").toInstant());
                });
    }

    @Override public Optional<PlacementAssignment> findReusable(UUID userId, PlacementPool pool, Instant since) {
        return jdbc.query("SELECT " + COLUMNS + " FROM placement_assignments WHERE user_id=:userId AND pool=:pool " +
                        "AND assigned_at >= :since AND completed_at IS NULL AND released_at IS NULL " +
                        "ORDER BY assigned_at DESC LIMIT 1",
                Map.of("userId", userId, "pool", pool.name(), "since", Timestamp.from(since)), ROW).stream().findFirst();
    }

    @Override public Optional<PlacementAssignment> findOpen(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM placement_assignments WHERE user_id=:userId " +
                        "AND completed_at IS NULL AND released_at IS NULL ORDER BY assigned_at DESC LIMIT 1",
                Map.of("userId", userId), ROW).stream().findFirst();
    }

    @Override public Map<UUID, Instant> latestByPackage(UUID userId) {
        Map<UUID, Instant> result = new HashMap<>();
        jdbc.query("SELECT package_id,max(assigned_at) AS latest FROM placement_assignments " +
                        "WHERE user_id=:userId AND released_at IS NULL GROUP BY package_id",
                Map.of("userId", userId), (RowCallbackHandler) rs -> result.put(rs.getObject("package_id", UUID.class),
                        rs.getTimestamp("latest").toInstant()));
        return result;
    }

    @Override public List<PlacementAssignment> findByUser(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM placement_assignments WHERE user_id=:userId " +
                "AND released_at IS NULL ORDER BY assigned_at DESC", Map.of("userId", userId), ROW);
    }

    @Override public Optional<PlacementAssignment> findOwnedOpen(UUID userId, UUID assignmentId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM placement_assignments WHERE user_id=:userId AND id=:id " +
                        "AND completed_at IS NULL AND released_at IS NULL FOR UPDATE",
                Map.of("userId", userId, "id", assignmentId), ROW).stream().findFirst();
    }

    @Override public Optional<PlacementAssignment> findOpenForResult(UUID userId, UUID versionId, Instant completedAt) {
        return jdbc.query("SELECT " + COLUMNS + " FROM placement_assignments WHERE user_id=:userId " +
                        "AND package_version_id=:versionId AND completed_at IS NULL AND released_at IS NULL " +
                        "AND assigned_at <= :completedAt ORDER BY assigned_at DESC LIMIT 1 FOR UPDATE",
                Map.of("userId", userId, "versionId", versionId, "completedAt", Timestamp.from(completedAt)), ROW)
                .stream().findFirst();
    }

    @Override public void save(PlacementAssignment a) {
        jdbc.update("INSERT INTO placement_assignments(id,user_id,package_id,package_version_id,pool,assigned_at,completed_at,released_at,attempt_id) " +
                        "VALUES(:id,:userId,:packageId,:versionId,:pool,:assignedAt,:completedAt,:releasedAt,:attemptId) " +
                        "ON CONFLICT(id) DO UPDATE SET completed_at=EXCLUDED.completed_at,released_at=EXCLUDED.released_at,attempt_id=EXCLUDED.attempt_id",
                new MapSqlParameterSource().addValue("id", a.id()).addValue("userId", a.userId())
                        .addValue("packageId", a.packageId()).addValue("versionId", a.packageVersionId())
                        .addValue("pool", a.pool().name()).addValue("assignedAt", Timestamp.from(a.assignedAt()))
                        .addValue("completedAt", a.completedAt() == null ? null : Timestamp.from(a.completedAt()))
                        .addValue("releasedAt", a.releasedAt() == null ? null : Timestamp.from(a.releasedAt()))
                        .addValue("attemptId", a.attemptId()));
    }
}
