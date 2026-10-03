package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.domain.aggregate.PracticeAttempt;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.domain.vo.LearningSkill;
import com.group01.learning.domain.vo.PracticeSubmission;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcPracticeAttemptRepository implements PracticeAttemptRepository {
    private static final String COLUMNS = "id, user_id, lesson_id, skill, package_id, package_version_id, "
            + "started_at, submitted_at, request_id, response";
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    private final RowMapper<PracticeAttempt> mapper = this::map;

    public JdbcPracticeAttemptRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<PracticeAttempt> findOwned(UUID userId, UUID attemptId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM practice_attempts WHERE id = :id AND user_id = :userId",
                Map.of("id", attemptId, "userId", userId), mapper).stream().findFirst();
    }

    @Override
    public Optional<PracticeAttempt> findOpen(UUID userId, UUID packageId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM practice_attempts "
                + "WHERE user_id = :userId AND package_id = :packageId AND submitted_at IS NULL",
                Map.of("userId", userId, "packageId", packageId), mapper).stream().findFirst();
    }

    @Override
    public Optional<PracticeAttempt> findByRequestId(UUID requestId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM practice_attempts WHERE request_id = :requestId",
                Map.of("requestId", requestId), mapper).stream().findFirst();
    }

    @Override
    public List<PracticeAttempt> findByLessons(UUID userId, Collection<UUID> lessonIds) {
        if (lessonIds.isEmpty()) return List.of();
        return jdbc.query("SELECT " + COLUMNS + " FROM practice_attempts "
                + "WHERE user_id = :userId AND lesson_id IN (:lessonIds) ORDER BY started_at DESC, id",
                Map.of("userId", userId, "lessonIds", lessonIds), mapper);
    }

    @Override
    public TopicAttempts findForTopic(UUID userId, Collection<UUID> lessonIds, Collection<UUID> packageIds) {
        if (lessonIds.isEmpty()) return new TopicAttempts(List.of(), Set.of());
        List<FirstPass> firstPasses = new ArrayList<>();
        Set<UUID> revealed = new HashSet<>();
        String sql = """
                SELECT 'PASS' AS row_kind, lesson_id, MIN(package_id::text)::uuid AS package_id
                FROM practice_attempts
                WHERE user_id = :userId AND lesson_id IN (:lessonIds)
                  AND submitted_at IS NOT NULL AND passed = TRUE AND counted_as_evidence = TRUE
                GROUP BY lesson_id
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("lessonIds", lessonIds);
        if (!packageIds.isEmpty()) {
            params.put("packageIds", packageIds);
            sql += """
                    UNION ALL
                    SELECT 'REVEALED', NULL::uuid, package_id FROM (
                        SELECT package_id FROM practice_attempts
                        WHERE user_id = :userId AND package_id IN (:packageIds) AND submitted_at IS NOT NULL
                        UNION
                        SELECT package_id FROM review_sets
                        WHERE user_id = :userId AND package_id IN (:packageIds) AND submitted_at IS NOT NULL
                    ) revealed_packages
                    """;
        }
        jdbc.query(sql, params, row -> {
            if ("PASS".equals(row.getString("row_kind"))) {
                firstPasses.add(new FirstPass(row.getObject("lesson_id", UUID.class),
                        row.getObject("package_id", UUID.class)));
            } else revealed.add(row.getObject("package_id", UUID.class));
        });
        return new TopicAttempts(List.copyOf(firstPasses), Set.copyOf(revealed));
    }

    @Override
    public List<PackageSummary> summarizeForLesson(UUID userId, UUID lessonId, Collection<UUID> packageIds) {
        if (packageIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT package_id,
                       (array_agg(id ORDER BY started_at DESC, id))[1] AS last_attempt_id,
                       bool_or(submitted_at IS NULL) AS has_open,
                       bool_or(passed IS TRUE) AS has_passed,
                       bool_or(submitted_at IS NOT NULL) AS has_attempted,
                       max(correct_count::double precision / NULLIF(total_count, 0)) AS best_percent
                FROM practice_attempts
                WHERE user_id = :userId AND lesson_id = :lessonId AND package_id IN (:packageIds)
                GROUP BY package_id
                """, Map.of("userId", userId, "lessonId", lessonId, "packageIds", packageIds), (row, index) ->
                new PackageSummary(row.getObject("package_id", UUID.class),
                        row.getObject("last_attempt_id", UUID.class), row.getBoolean("has_open"),
                        row.getBoolean("has_passed"), row.getBoolean("has_attempted"),
                        (Double) row.getObject("best_percent")));
    }

    @Override
    public Set<UUID> revealedPackageIds(UUID userId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT package_id FROM practice_attempts WHERE user_id = :userId AND submitted_at IS NOT NULL
                UNION
                SELECT package_id FROM review_sets WHERE user_id = :userId AND submitted_at IS NOT NULL
                """, Map.of("userId", userId), UUID.class));
    }

    @Override
    public Set<UUID> revealedPackageIds(UUID userId, Collection<UUID> packageIds) {
        if (packageIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("""
                SELECT package_id FROM practice_attempts
                WHERE user_id = :userId AND package_id IN (:packageIds) AND submitted_at IS NOT NULL
                UNION
                SELECT package_id FROM review_sets
                WHERE user_id = :userId AND package_id IN (:packageIds) AND submitted_at IS NOT NULL
                """, Map.of("userId", userId, "packageIds", packageIds), UUID.class));
    }

    @Override
    public void insert(PracticeAttempt attempt) {
        jdbc.update("""
                INSERT INTO practice_attempts (id, user_id, lesson_id, skill, package_id, package_version_id, started_at)
                VALUES (:id, :userId, :lessonId, :skill, :packageId, :versionId, :startedAt)
                """, params(attempt));
    }

    @Override
    public void saveResult(PracticeAttempt attempt) {
        Map<String, Object> params = params(attempt);
        params.put("requestId", attempt.requestId());
        params.put("submittedAt", Timestamp.from(attempt.submittedAt()));
        params.put("correct", attempt.response().correct());
        params.put("total", attempt.response().total());
        params.put("passed", attempt.response().passed());
        params.put("counted", attempt.response().countedAsEvidence());
        try {
            params.put("response", json.writeValueAsString(attempt.response()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode practice response", exception);
        }
        int updated = jdbc.update("""
                UPDATE practice_attempts SET submitted_at = :submittedAt, request_id = :requestId,
                correct_count = :correct, total_count = :total, passed = :passed,
                counted_as_evidence = :counted, response = CAST(:response AS jsonb)
                WHERE id = :id AND user_id = :userId AND submitted_at IS NULL
                """, params);
        if (updated != 1) throw new IllegalStateException("Practice attempt changed while submitting");
    }

    private Map<String, Object> params(PracticeAttempt attempt) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", attempt.id());
        params.put("userId", attempt.userId());
        params.put("lessonId", attempt.lessonId());
        params.put("skill", attempt.skill().name());
        params.put("packageId", attempt.packageId());
        params.put("versionId", attempt.packageVersionId());
        params.put("startedAt", Timestamp.from(attempt.startedAt()));
        return params;
    }

    private PracticeAttempt map(ResultSet row, int index) throws SQLException {
        String responseJson = row.getString("response");
        PracticeSubmission response = null;
        if (responseJson != null) {
            try {
                response = json.readValue(responseJson, PracticeSubmission.class);
            } catch (JsonProcessingException exception) {
                throw new SQLException("Cannot decode practice response", exception);
            }
        }
        Instant submitted = row.getTimestamp("submitted_at") == null ? null
                : row.getTimestamp("submitted_at").toInstant();
        return PracticeAttempt.restore(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getObject("lesson_id", UUID.class), LearningSkill.valueOf(row.getString("skill")),
                row.getObject("package_id", UUID.class), row.getObject("package_version_id", UUID.class),
                row.getTimestamp("started_at").toInstant(), submitted, row.getObject("request_id", UUID.class),
                response);
    }
}
