package com.ieltspath.assessment.application.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only views behind the AI evaluation screens: the gold essays, the evaluation runs and the prompt version each
 * task currently uses. Paged and filtered in the database. Essay and prompt text are cut to a preview, and nothing
 * here is logged.
 */
@Service
public class AiEvaluationReadService {
    static final int MAX_PAGE_SIZE = 100;
    static final int ESSAY_PREVIEW_LENGTH = 200;
    private static final List<String> TASKS = List.of("TASK_1", "TASK_2");

    private final JdbcTemplate jdbc;

    public AiEvaluationReadService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Page<GoldEssayRow> goldEssays(String task, int page, int size) {
        requireTask(task);
        requirePage(page, size);
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM ai_gold_essays WHERE (?::varchar IS NULL OR task = ?)", Long.class, task, task);
        List<GoldEssayRow> rows = jdbc.query("""
                SELECT id, task, examiner_band, source, left(essay, ?) AS essay_preview, created_at
                FROM ai_gold_essays WHERE (?::varchar IS NULL OR task = ?)
                ORDER BY created_at DESC, id LIMIT ? OFFSET ?
                """, (rs, row) -> new GoldEssayRow(rs.getObject("id", UUID.class), rs.getString("task"),
                rs.getBigDecimal("examiner_band"), rs.getString("source"), rs.getString("essay_preview"),
                rs.getTimestamp("created_at").toInstant()),
                ESSAY_PREVIEW_LENGTH, task, task, size, (long) page * size);
        return new Page<>(rows, page, size, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public Page<EvaluationRow> evaluations(String task, int page, int size) {
        requireTask(task);
        requirePage(page, size);
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM ai_eval_runs WHERE (?::varchar IS NULL OR task = ?)", Long.class, task, task);
        List<EvaluationRow> rows = jdbc.query("""
                SELECT id, task, model, prompt_version, status, n, within_half_band, mae, qwk, passed, started_at,
                       finished_at
                FROM ai_eval_runs WHERE (?::varchar IS NULL OR task = ?)
                ORDER BY started_at DESC, id LIMIT ? OFFSET ?
                """, (rs, row) -> new EvaluationRow(rs.getObject("id", UUID.class), rs.getString("task"),
                rs.getString("model"), rs.getString("prompt_version"), rs.getString("status"), rs.getInt("n"),
                rs.getBigDecimal("within_half_band"), rs.getBigDecimal("mae"), rs.getBigDecimal("qwk"),
                (Boolean) rs.getObject("passed"), rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant()),
                task, task, size, (long) page * size);
        return new Page<>(rows, page, size, total == null ? 0 : total);
    }

    /**
     * One entry per task, in task order. A task that was never activated has null fields: it is still graded by the
     * {@code ACTIVE} rubric of Content, not by a prompt that passed an evaluation.
     */
    @Transactional(readOnly = true)
    public List<ActivationRow> activations() {
        return TASKS.stream().map(task -> jdbc.query("""
                SELECT prompt_version, run_id, activated_at FROM ai_prompt_activations WHERE task = ?
                """, (rs, row) -> new ActivationRow(task, rs.getString("prompt_version"),
                rs.getObject("run_id", UUID.class), rs.getTimestamp("activated_at").toInstant()), task)
                .stream().findFirst().orElse(new ActivationRow(task, null, null, null))).toList();
    }

    private static void requireTask(String task) {
        if (task != null && !TASKS.contains(task)) {
            throw new IllegalArgumentException("task must be TASK_1 or TASK_2");
        }
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid pagination (size 1.." + MAX_PAGE_SIZE + ")");
        }
    }

    public record Page<T>(List<T> content, int page, int size, long totalElements) {}

    public record GoldEssayRow(UUID id, String task, BigDecimal examinerBand, String source, String essayPreview,
                               Instant createdAt) {}

    public record EvaluationRow(UUID id, String task, String model, String promptVersion, String status, int n,
                                BigDecimal withinHalfBand, BigDecimal mae, BigDecimal qwk, Boolean passed,
                                Instant startedAt, Instant finishedAt) {}

    public record ActivationRow(String task, String promptVersion, UUID runId, Instant activatedAt) {}
}
