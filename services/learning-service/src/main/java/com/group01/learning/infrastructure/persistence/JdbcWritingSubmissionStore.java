package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.port.WritingSubmissionStore;
import com.group01.learning.application.writing.EssayPrompt;
import com.group01.learning.application.writing.WritingGrade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JdbcWritingSubmissionStore implements WritingSubmissionStore {
    private static final String COLUMNS = """
            id, user_id, lesson_id, block_id, request_id, essay_text, word_count, prompt_snapshot::text AS prompt,
            status, point_cost, debit_ledger_entry_id, failure_code, result::text AS result, overall_band, passed,
            grading_started_at""";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcWritingSubmissionStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Submission> findByRequestId(UUID requestId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE request_id = ?",
                this::submission, requestId).stream().findFirst();
    }

    @Override
    public Optional<Submission> findForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE id = ? FOR UPDATE",
                this::submission, id).stream().findFirst();
    }

    @Override
    public Optional<Submission> findOwned(UUID id, UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE id = ? AND user_id = ?",
                this::submission, id, userId).stream().findFirst();
    }

    @Override
    public void abandonStaleGrading(UUID userId, UUID blockId, Instant staleBefore) {
        jdbc.update("""
                UPDATE lesson_writing_submissions SET status = 'FAILED', failure_code = 'GRADING_ABANDONED'
                WHERE user_id = ? AND block_id = ? AND status = 'GRADING' AND grading_started_at < ?
                """, userId, blockId, Timestamp.from(staleBefore));
    }

    @Override
    public boolean hasGrading(UUID userId, UUID blockId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM lesson_writing_submissions
                WHERE user_id = ? AND block_id = ? AND status = 'GRADING')
                """, Boolean.class, userId, blockId));
    }

    @Override
    public void insertGrading(NewSubmission submission) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                    knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    grading_started_at, submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'GRADING', ?, ?, ?)
                    """);
            Timestamp now = Timestamp.from(submission.now());
            statement.setObject(1, submission.id());
            statement.setObject(2, submission.userId());
            statement.setObject(3, submission.lessonId());
            statement.setObject(4, submission.blockId());
            statement.setObject(5, submission.prompt().questionVersionId());
            statement.setArray(6, connection.createArrayOf("uuid", submission.prompt().knowledgePointIds().toArray()));
            statement.setObject(7, submission.requestId());
            statement.setString(8, submission.essayText());
            statement.setInt(9, submission.wordCount());
            statement.setString(10, write(submission.prompt()));
            statement.setInt(11, submission.pointCost());
            statement.setTimestamp(12, now);
            statement.setTimestamp(13, now);
            return statement;
        });
    }

    @Override
    public boolean restartGrading(UUID id, Instant now) {
        return jdbc.update("""
                UPDATE lesson_writing_submissions SET status = 'GRADING', failure_code = NULL, grading_started_at = ?
                WHERE id = ? AND status = 'FAILED'
                """, Timestamp.from(now), id) == 1;
    }

    @Override
    public boolean incrementDailyUsage(UUID userId, LocalDate day, String kind, int limit) {
        return jdbc.update("""
                INSERT INTO llm_daily_usage (user_id, usage_date, kind, count) VALUES (?, ?, ?, 1)
                ON CONFLICT (user_id, usage_date, kind) DO UPDATE SET count = llm_daily_usage.count + 1
                WHERE llm_daily_usage.count < ?
                """, userId, day, kind, limit) == 1;
    }

    @Override
    public void markFailed(UUID id, String failureCode) {
        jdbc.update("UPDATE lesson_writing_submissions SET status = 'FAILED', failure_code = ? "
                + "WHERE id = ? AND status = 'GRADING'", failureCode, id);
    }

    @Override
    public boolean saveGrade(UUID id, WritingGrade grade, boolean passed) {
        return jdbc.update("""
                UPDATE lesson_writing_submissions SET status = 'PAYMENT_PENDING', result = CAST(? AS jsonb),
                overall_band = ?, passed = ?, failure_code = NULL WHERE id = ? AND status = 'GRADING'
                """, write(new StoredGrade(grade.criteria(), grade.corrections(), grade.summary())),
                grade.overallBand(), passed, id) == 1;
    }

    @Override
    public void recordPaymentFailure(UUID id, String code) {
        jdbc.update("UPDATE lesson_writing_submissions SET failure_code = ? WHERE id = ? AND status = 'PAYMENT_PENDING'",
                code, id);
    }

    @Override
    public void markGraded(UUID id, UUID ledgerEntryId) {
        jdbc.update("""
                UPDATE lesson_writing_submissions SET status = 'GRADED', debit_ledger_entry_id = ?, failure_code = NULL,
                graded_at = clock_timestamp() WHERE id = ? AND status = 'PAYMENT_PENDING'
                """, ledgerEntryId, id);
    }

    @Override
    public boolean blockPassed(UUID userId, UUID blockId, UUID excludeId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM lesson_writing_submissions WHERE user_id = ? AND block_id = ?
                AND status = 'GRADED' AND passed AND id <> ?)
                """, Boolean.class, userId, blockId, excludeId));
    }

    @Override
    public Map<UUID, BlockSummary> summarizeBlocks(UUID userId, Collection<UUID> blockIds) {
        if (blockIds.isEmpty()) return Map.of();
        Map<UUID, BlockSummary> summaries = new HashMap<>();
        jdbc.query(connection -> {
            var statement = connection.prepareStatement("""
                    SELECT DISTINCT ON (block_id) block_id, id, status, overall_band, passed,
                    bool_or(status = 'GRADED' AND passed) OVER (PARTITION BY block_id) AS block_passed
                    FROM lesson_writing_submissions WHERE user_id = ? AND block_id = ANY (?)
                    ORDER BY block_id, submitted_at DESC, id
                    """);
            statement.setObject(1, userId);
            statement.setArray(2, connection.createArrayOf("uuid", blockIds.toArray()));
            return statement;
        }, row -> {
            boolean graded = "GRADED".equals(row.getString("status"));
            summaries.put(row.getObject("block_id", UUID.class), new BlockSummary(row.getObject("id", UUID.class),
                    row.getString("status"), graded ? row.getBigDecimal("overall_band") : null,
                    graded ? (Boolean) row.getObject("passed") : null, row.getBoolean("block_passed")));
        });
        return summaries;
    }

    private Submission submission(ResultSet row, int index) throws SQLException {
        StoredGrade stored = read(row.getString("result"), StoredGrade.class);
        var band = row.getBigDecimal("overall_band");
        return new Submission(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getObject("lesson_id", UUID.class), row.getObject("block_id", UUID.class),
                row.getObject("request_id", UUID.class), row.getString("essay_text"), row.getInt("word_count"),
                read(row.getString("prompt"), EssayPrompt.class), row.getString("status"), row.getInt("point_cost"),
                row.getObject("debit_ledger_entry_id", UUID.class), row.getString("failure_code"),
                stored == null ? null : new WritingGrade(stored.criteria(), stored.corrections(), stored.summary(), band),
                band, (Boolean) row.getObject("passed"), row.getTimestamp("grading_started_at").toInstant());
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Writing submission could not be serialized", exception);
        }
    }

    private <T> T read(String value, Class<T> type) {
        if (value == null) return null;
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored writing submission is unreadable", exception);
        }
    }

    /** The overall band lives in its own column; the JSON keeps the criteria and feedback. */
    private record StoredGrade(List<WritingGrade.Criterion> criteria, List<WritingGrade.Correction> corrections,
                               String summary) {}
}
