package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcMasteryProfileRepository implements MasteryProfileRepository {
    private static final String COLUMNS = """
            user_id, knowledge_point_id, mastery, tier, evidence_count, success_streak, last_evidence_at,
            next_review_at, median_duration_ms, top_error_codes, confidence""";
    private static final RowMapper<MasteryProfile> MAPPER = JdbcMasteryProfileRepository::map;
    private final JdbcTemplate jdbc;

    public JdbcMasteryProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, MasteryProfile> find(UUID userId, Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, MasteryProfile> found = new LinkedHashMap<>();
        jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ANY (?)",
                statement -> {
                    statement.setObject(1, userId);
                    statement.setArray(2, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(),
                            knowledgePointIds));
                }, rows -> {
                    MasteryProfile profile = map(rows, rows.getRow());
                    found.put(profile.knowledgePointId(), profile);
                });
        return found;
    }

    @Override
    public List<MasteryProfile> findAll(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles p WHERE user_id = ? " +
                        "AND NOT EXISTS (SELECT 1 FROM knowledge_point_catalog c " +
                        "WHERE c.kp_id = p.knowledge_point_id AND c.parent_kp_id IS NOT NULL) " +
                        "ORDER BY knowledge_point_id",
                MAPPER, userId);
    }

    @Override
    public int countDue(UUID userId, Instant now, Collection<UUID> excludedKnowledgePointIds) {
        Integer due = jdbc.query("SELECT count(*) FROM kp_mastery_profiles p WHERE p.user_id = ? "
                        + "AND p.tier = 'MAINTAIN' AND p.next_review_at IS NOT NULL AND p.next_review_at <= ? "
                        + "AND NOT (p.knowledge_point_id = ANY (?)) "
                        + "AND NOT EXISTS (SELECT 1 FROM knowledge_point_catalog c "
                        + "WHERE c.kp_id = p.knowledge_point_id AND c.parent_kp_id IS NOT NULL)",
                statement -> {
                    statement.setObject(1, userId);
                    statement.setTimestamp(2, Timestamp.from(now));
                    statement.setArray(3, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(),
                            excludedKnowledgePointIds));
                }, rows -> rows.next() ? rows.getInt(1) : 0);
        return due == null ? 0 : due;
    }

    @Override
    public List<MasteryProfile> findErrorTrapProfiles(UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles p "
                        + "JOIN knowledge_point_catalog c ON c.kp_id = p.knowledge_point_id "
                        + "WHERE p.user_id = ? AND c.parent_kp_id IS NOT NULL ORDER BY p.knowledge_point_id",
                MAPPER, userId);
    }

    @Override
    public void upsert(List<MasteryProfile> profiles) {
        if (profiles.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO kp_mastery_profiles (user_id, knowledge_point_id, mastery, tier, evidence_count,
                    success_streak, last_evidence_at, next_review_at, median_duration_ms, top_error_codes, confidence,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, knowledge_point_id) DO UPDATE SET mastery = EXCLUDED.mastery,
                    tier = EXCLUDED.tier, evidence_count = EXCLUDED.evidence_count,
                    success_streak = EXCLUDED.success_streak, last_evidence_at = EXCLUDED.last_evidence_at,
                    next_review_at = EXCLUDED.next_review_at, median_duration_ms = EXCLUDED.median_duration_ms,
                    top_error_codes = EXCLUDED.top_error_codes, confidence = EXCLUDED.confidence,
                    updated_at = CURRENT_TIMESTAMP
                """, profiles, profiles.size(), (statement, profile) -> {
            statement.setObject(1, profile.userId());
            statement.setObject(2, profile.knowledgePointId());
            statement.setBigDecimal(3, BigDecimal.valueOf(profile.mastery()).setScale(3, java.math.RoundingMode.HALF_UP));
            statement.setString(4, profile.tier().name());
            statement.setInt(5, profile.evidenceCount());
            statement.setInt(6, profile.successStreak());
            statement.setTimestamp(7, Timestamp.from(profile.lastEvidenceAt()));
            if (profile.nextReviewAt() == null) statement.setNull(8, Types.TIMESTAMP_WITH_TIMEZONE);
            else statement.setTimestamp(8, Timestamp.from(profile.nextReviewAt()));
            statement.setObject(9, profile.medianDurationMs(), Types.BIGINT);
            statement.setArray(10, statement.getConnection().createArrayOf("varchar",
                    profile.topErrorCodes().toArray()));
            if (profile.confidence() == null) statement.setNull(11, Types.NUMERIC);
            else statement.setBigDecimal(11, BigDecimal.valueOf(profile.confidence())
                    .setScale(3, java.math.RoundingMode.HALF_UP));
        });
    }

    @Override
    public void reclassify(List<MasteryProfile> profiles) {
        if (profiles.isEmpty()) return;
        jdbc.batchUpdate("""
                UPDATE kp_mastery_profiles SET tier = ?, next_review_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND knowledge_point_id = ? AND mastery = ? AND evidence_count = ?
                    AND success_streak = ?
                """, profiles, profiles.size(), (statement, profile) -> {
            statement.setString(1, profile.tier().name());
            if (profile.nextReviewAt() == null) statement.setNull(2, Types.TIMESTAMP_WITH_TIMEZONE);
            else statement.setTimestamp(2, Timestamp.from(profile.nextReviewAt()));
            statement.setObject(3, profile.userId());
            statement.setObject(4, profile.knowledgePointId());
            statement.setBigDecimal(5, BigDecimal.valueOf(profile.mastery()).setScale(3, java.math.RoundingMode.HALF_UP));
            statement.setInt(6, profile.evidenceCount());
            statement.setInt(7, profile.successStreak());
        });
    }

    @Override
    public void delete(UUID userId, Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) return;
        jdbc.update("DELETE FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ANY (?)", statement -> {
            statement.setObject(1, userId);
            statement.setArray(2, JdbcKnowledgeEvidenceRepository.uuidArray(statement.getConnection(),
                    knowledgePointIds));
        });
    }

    @Override
    public List<UUID> usersWithUnprofiledEvidence() {
        return jdbc.queryForList("""
                SELECT DISTINCT evidence.user_id FROM kp_evidence evidence
                WHERE NOT EXISTS (SELECT 1 FROM kp_mastery_profiles profile
                    WHERE profile.user_id = evidence.user_id AND profile.knowledge_point_id = evidence.kp_id)
                """, UUID.class);
    }

    @Override
    public Set<UUID> unprofiledKnowledgePoints(UUID userId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT evidence.kp_id FROM kp_evidence evidence
                WHERE evidence.user_id = ? AND NOT EXISTS (SELECT 1 FROM kp_mastery_profiles profile
                    WHERE profile.user_id = evidence.user_id AND profile.knowledge_point_id = evidence.kp_id)
                """, UUID.class, userId));
    }

    @Override
    public List<MasteryProfile> findBatchAfter(UUID afterUserId, UUID afterKnowledgePointId, int limit) {
        if (afterUserId == null) {
            return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles "
                    + "ORDER BY user_id, knowledge_point_id LIMIT ?", MAPPER, limit);
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles "
                + "WHERE (user_id, knowledge_point_id) > (?, ?) ORDER BY user_id, knowledge_point_id LIMIT ?",
                MAPPER, afterUserId, afterKnowledgePointId, limit);
    }

    @Override
    public List<MasteryProfile> findStaleBatchAfter(java.time.Instant staleBefore, UUID afterUserId,
                                                    UUID afterKnowledgePointId, int limit) {
        Timestamp before = Timestamp.from(staleBefore);
        if (afterUserId == null) {
            return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles WHERE last_evidence_at < ? "
                    + "ORDER BY user_id, knowledge_point_id LIMIT ?", MAPPER, before, limit);
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM kp_mastery_profiles WHERE last_evidence_at < ? "
                + "AND (user_id, knowledge_point_id) > (?, ?) ORDER BY user_id, knowledge_point_id LIMIT ?",
                MAPPER, before, afterUserId, afterKnowledgePointId, limit);
    }

    private static MasteryProfile map(ResultSet rows, int rowNumber) throws SQLException {
        Timestamp next = rows.getTimestamp("next_review_at");
        long median = rows.getLong("median_duration_ms");
        Long medianMs = rows.wasNull() ? null : median;
        BigDecimal confidenceValue = rows.getBigDecimal("confidence");
        Object[] codes = (Object[]) rows.getArray("top_error_codes").getArray();
        return new MasteryProfile(rows.getObject("user_id", UUID.class),
                rows.getObject("knowledge_point_id", UUID.class), rows.getBigDecimal("mastery").doubleValue(),
                MasteryTier.valueOf(rows.getString("tier")), rows.getInt("evidence_count"),
                rows.getInt("success_streak"), rows.getTimestamp("last_evidence_at").toInstant(),
                next == null ? null : next.toInstant(), medianMs,
                Arrays.stream(codes).map(String::valueOf).toList(),
                confidenceValue == null ? null : confidenceValue.doubleValue());
    }
}
