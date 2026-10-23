package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAiDraftRepository implements AiDraftRepository {

    private static final String COLUMNS = """
            id, kind, status, requested_by, request::text AS request, result::text AS result, error_code, model,
            prompt_version, created_at, started_at, finished_at, reviewed_by, reviewed_at, review_note
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAiDraftRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public AiDraft save(AiDraft draft) {
        jdbc.update("""
                INSERT INTO ai_drafts (id, kind, status, requested_by, request, result, error_code, model,
                                       prompt_version, created_at, started_at, finished_at, reviewed_by,
                                       reviewed_at, review_note)
                VALUES (:id, :kind, :status, :requestedBy, CAST(:request AS JSONB), CAST(:result AS JSONB),
                        :errorCode, :model, :promptVersion, :createdAt, :startedAt, :finishedAt, :reviewedBy,
                        :reviewedAt, :reviewNote)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status, result = EXCLUDED.result, error_code = EXCLUDED.error_code,
                    model = EXCLUDED.model, prompt_version = EXCLUDED.prompt_version,
                    started_at = EXCLUDED.started_at, finished_at = EXCLUDED.finished_at,
                    reviewed_by = EXCLUDED.reviewed_by, reviewed_at = EXCLUDED.reviewed_at,
                    review_note = EXCLUDED.review_note
                """, new MapSqlParameterSource()
                .addValue("id", draft.getId())
                .addValue("kind", draft.getKind().name())
                .addValue("status", draft.getStatus().name())
                .addValue("requestedBy", draft.getRequestedBy())
                .addValue("request", draft.getRequestJson())
                .addValue("result", draft.getResultJson())
                .addValue("errorCode", draft.getErrorCode())
                .addValue("model", draft.getModel())
                .addValue("promptVersion", draft.getPromptVersion())
                .addValue("createdAt", timestamp(draft.getCreatedAt()))
                .addValue("startedAt", timestamp(draft.getStartedAt()))
                .addValue("finishedAt", timestamp(draft.getFinishedAt()))
                .addValue("reviewedBy", draft.getReviewedBy())
                .addValue("reviewedAt", timestamp(draft.getReviewedAt()))
                .addValue("reviewNote", draft.getReviewNote()));
        return findById(draft.getId()).orElseThrow();
    }

    @Override
    public Optional<AiDraft> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ai_drafts WHERE id = :id",
                        new MapSqlParameterSource("id", id), JdbcAiDraftRepository::map)
                .stream().findFirst();
    }

    @Override
    public List<AiDraft> findByRequester(UUID requestedBy, DraftKind kind, DraftStatus status, int page, int size) {
        return jdbc.query("""
                SELECT %s FROM ai_drafts
                WHERE requested_by = :requestedBy
                  AND (CAST(:kind AS VARCHAR) IS NULL OR kind = CAST(:kind AS VARCHAR))
                  AND (CAST(:status AS VARCHAR) IS NULL OR status = CAST(:status AS VARCHAR))
                ORDER BY created_at DESC, id
                LIMIT :size OFFSET :offset
                """.formatted(COLUMNS), new MapSqlParameterSource()
                .addValue("requestedBy", requestedBy)
                .addValue("kind", kind == null ? null : kind.name())
                .addValue("status", status == null ? null : status.name())
                .addValue("size", size)
                .addValue("offset", page * size), JdbcAiDraftRepository::map);
    }

    /**
     * The inner select locks the chosen rows and skips rows another worker already holds, so two workers claiming at
     * once split the queue instead of both taking the same draft.
     */
    @Override
    @Transactional
    public List<UUID> claimQueued(int limit) {
        return jdbc.queryForList("""
                WITH picked AS (
                    SELECT id FROM ai_drafts
                    WHERE status = 'QUEUED'
                    ORDER BY created_at, id
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE ai_drafts d
                SET status = 'GENERATING', started_at = CURRENT_TIMESTAMP
                FROM picked
                WHERE d.id = picked.id
                RETURNING d.id
                """, new MapSqlParameterSource("limit", limit), UUID.class);
    }

    @Override
    @Transactional
    public int requeueStaleGenerating(Instant startedBefore) {
        return jdbc.update("""
                UPDATE ai_drafts SET status = 'QUEUED', started_at = NULL
                WHERE status = 'GENERATING' AND started_at < :before
                """, new MapSqlParameterSource("before", timestamp(startedBefore)));
    }

    private static AiDraft map(ResultSet rs, int row) throws SQLException {
        return new AiDraft(
                rs.getObject("id", UUID.class),
                DraftKind.valueOf(rs.getString("kind")),
                DraftStatus.valueOf(rs.getString("status")),
                rs.getObject("requested_by", UUID.class),
                rs.getString("request"),
                rs.getString("result"),
                rs.getString("error_code"),
                rs.getString("model"),
                rs.getString("prompt_version"),
                instant(rs, "created_at"),
                instant(rs, "started_at"),
                instant(rs, "finished_at"),
                rs.getObject("reviewed_by", UUID.class),
                instant(rs, "reviewed_at"),
                rs.getString("review_note"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
