package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.AiDraftClaim;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
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
    public Optional<AiDraft> findOpenSectionDraft(UUID authorId, UUID sectionId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ai_drafts " + """
                WHERE requested_by = :authorId AND kind = 'SECTION_QUESTIONS'
                  AND request ->> 'sectionId' = :sectionId
                  AND status IN ('QUEUED', 'GENERATING', 'READY')
                ORDER BY created_at DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("authorId", authorId)
                .addValue("sectionId", sectionId.toString()), JdbcAiDraftRepository::map).stream().findFirst();
    }

    @Override
    public Optional<AiDraft> findOpenLessonQuestionDraft(UUID authorId, UUID lessonId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ai_drafts " + """
                WHERE requested_by = :authorId AND kind = 'QUESTIONS'
                  AND request ->> 'lessonId' = :lessonId
                  AND status IN ('QUEUED', 'GENERATING', 'READY')
                ORDER BY created_at DESC, id LIMIT 1
                """, new MapSqlParameterSource().addValue("authorId", authorId)
                .addValue("lessonId", lessonId.toString()), JdbcAiDraftRepository::map).stream().findFirst();
    }

    @Override
    @Transactional
    public Optional<AiDraft> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ai_drafts WHERE id = :id FOR UPDATE",
                        new MapSqlParameterSource("id", id), JdbcAiDraftRepository::map)
                .stream().findFirst();
    }

    @Override
    public int deleteIfStatusIn(UUID id, Collection<DraftStatus> statuses) {
        if (statuses.isEmpty()) {
            return 0;
        }
        return jdbc.update("DELETE FROM ai_drafts WHERE id = :id AND status IN (:statuses)",
                new MapSqlParameterSource("id", id)
                        .addValue("statuses", statuses.stream().map(DraftStatus::name).toList()));
    }

    @Override
    public List<AiDraft> findByRequester(UUID requestedBy, DraftKind kind, DraftStatus status, int page, int size) {
        return findByRequester(requestedBy, kind == null ? List.of() : List.of(kind), status, page, size);
    }

    @Override
    public List<AiDraft> findByRequester(UUID requestedBy, List<DraftKind> kinds, DraftStatus status, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT %s FROM ai_drafts WHERE requested_by = :requestedBy".formatted(COLUMNS));
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("requestedBy", requestedBy);
        if (kinds != null && !kinds.isEmpty()) {
            sql.append(" AND kind IN (:kinds)");
            parameters.addValue("kinds", kinds.stream().map(DraftKind::name).toList());
        }
        if (status != null) {
            sql.append(" AND status = :status");
            parameters.addValue("status", status.name());
        }
        sql.append(" ORDER BY created_at DESC, id LIMIT :size OFFSET :offset");
        parameters.addValue("size", size).addValue("offset", page * size);
        return jdbc.query(sql.toString(), parameters, JdbcAiDraftRepository::map);
    }

    /**
     * The inner select locks the chosen rows and skips rows another worker already holds, so two workers claiming at
     * once split the queue instead of both taking the same draft.
     */
    @Override
    @Transactional
    public List<AiDraftClaim> claimQueued(int limit) {
        return jdbc.query("""
                WITH picked AS (
                    SELECT id FROM ai_drafts
                    WHERE status = 'QUEUED'
                    ORDER BY created_at, id
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE ai_drafts d
                SET status = 'GENERATING', started_at = CURRENT_TIMESTAMP,
                    claim_token = gen_random_uuid(), attempt_count = d.attempt_count + 1
                FROM picked
                WHERE d.id = picked.id
                RETURNING d.id, d.claim_token
                """, new MapSqlParameterSource("limit", limit),
                (rs, row) -> new AiDraftClaim(rs.getObject("id", UUID.class), rs.getObject("claim_token", UUID.class)));
    }

    @Override
    @Transactional
    public int completeIfClaimed(UUID id, UUID claimToken, String result, String model, String promptVersion,
                                 Instant now) {
        return jdbc.update("""
                UPDATE ai_drafts
                SET status = 'READY', result = CAST(:result AS JSONB), model = :model,
                    prompt_version = :promptVersion, finished_at = :now, error_code = NULL, claim_token = NULL
                WHERE id = :id AND status = 'GENERATING' AND claim_token = :claimToken
                """, new MapSqlParameterSource("id", id).addValue("claimToken", claimToken)
                .addValue("result", result).addValue("model", model).addValue("promptVersion", promptVersion)
                .addValue("now", timestamp(now)));
    }

    @Override
    @Transactional
    public int failIfClaimed(UUID id, UUID claimToken, String code, Instant now) {
        return jdbc.update("""
                UPDATE ai_drafts
                SET status = 'FAILED', error_code = :code, finished_at = :now, claim_token = NULL
                WHERE id = :id AND status = 'GENERATING' AND claim_token = :claimToken
                """, new MapSqlParameterSource("id", id).addValue("claimToken", claimToken)
                .addValue("code", code).addValue("now", timestamp(now)));
    }

    @Override
    @Transactional
    public List<AiDraft> requeueStaleGenerating(Instant startedBefore, int maxAttempts, Instant now) {
        return jdbc.query("""
                WITH stale AS (
                    SELECT id FROM ai_drafts
                    WHERE status = 'GENERATING' AND started_at < :before
                    ORDER BY started_at, id
                    FOR UPDATE SKIP LOCKED
                ), updated AS (
                    UPDATE ai_drafts d
                    SET status = CASE WHEN d.attempt_count >= :maxAttempts THEN 'FAILED' ELSE 'QUEUED' END,
                        error_code = CASE WHEN d.attempt_count >= :maxAttempts THEN 'INTERNAL' ELSE d.error_code END,
                        finished_at = CASE WHEN d.attempt_count >= :maxAttempts THEN :now ELSE d.finished_at END,
                        started_at = NULL, claim_token = NULL
                    FROM stale
                    WHERE d.id = stale.id
                    RETURNING d.id, d.kind, d.status, d.requested_by, d.request::text AS request,
                              d.result::text AS result, d.error_code, d.model, d.prompt_version, d.created_at,
                              d.started_at, d.finished_at, d.reviewed_by, d.reviewed_at, d.review_note
                )
                SELECT * FROM updated WHERE status = 'FAILED'
                """, new MapSqlParameterSource("before", timestamp(startedBefore))
                .addValue("maxAttempts", maxAttempts).addValue("now", timestamp(now)),
                JdbcAiDraftRepository::map);
    }

    @Override
    public int expireReadySectionDrafts(Instant finishedBefore) {
        return jdbc.update("""
                UPDATE ai_drafts SET status = 'EXPIRED'
                WHERE status = 'READY' AND finished_at < :finishedBefore
                  AND (kind = 'SECTION_QUESTIONS' OR (kind = 'QUESTIONS' AND jsonb_exists(request, 'lessonId')))
                """, new MapSqlParameterSource("finishedBefore", timestamp(finishedBefore)));
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
