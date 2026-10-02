package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.domain.aggregate.WritingSubmission;
import com.group01.learning.domain.repository.WritingSubmissionRepository;
import com.group01.learning.domain.vo.EssayPrompt;
import com.group01.learning.domain.vo.WritingGrade;
import com.group01.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class JdbcWritingSubmissionRepository implements WritingSubmissionRepository {
    private static final String COLUMNS = """
            id, user_id, lesson_id, block_id, request_id, essay_text, word_count, prompt_snapshot::text AS prompt,
            status, point_cost, debit_ledger_entry_id, failure_code, result::text AS result, overall_band, passed,
            grading_started_at""";

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcWritingSubmissionRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<WritingSubmission> findByRequestId(UUID requestId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE request_id = ?",
                this::submission, requestId).stream().findFirst();
    }

    @Override
    public Optional<WritingSubmission> findForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE id = ? FOR UPDATE",
                this::submission, id).stream().findFirst();
    }

    @Override
    public Optional<WritingSubmission> findOwned(UUID id, UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions WHERE id = ? AND user_id = ?",
                this::submission, id, userId).stream().findFirst();
    }

    @Override
    public List<WritingSubmission> findGrading(UUID userId, UUID blockId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM lesson_writing_submissions "
                + "WHERE user_id = ? AND block_id = ? AND status = 'GRADING'", this::submission, userId, blockId);
    }

    @Override
    public boolean save(WritingSubmission submission) {
        boolean written = submission.isNew() ? insert(submission) : update(submission);
        if (written) submission.persisted();
        return written;
    }

    private boolean insert(WritingSubmission submission) {
        return jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO lesson_writing_submissions (id, user_id, lesson_id, block_id, question_version_id,
                    knowledge_point_ids, request_id, essay_text, word_count, prompt_snapshot, status, point_cost,
                    grading_started_at, submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                    """);
            Timestamp started = Timestamp.from(submission.gradingStartedAt());
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
            statement.setString(11, submission.status().name());
            statement.setInt(12, submission.pointCost());
            statement.setTimestamp(13, started);
            statement.setTimestamp(14, started);
            return statement;
        }) == 1;
    }

    /** Compare-and-set on the status the submission was loaded with. */
    private boolean update(WritingSubmission submission) {
        WritingGrade grade = submission.grade();
        return jdbc.update("""
                UPDATE lesson_writing_submissions SET status = ?, failure_code = ?, result = CAST(? AS jsonb),
                overall_band = ?, passed = ?, debit_ledger_entry_id = ?, grading_started_at = ?,
                graded_at = CASE WHEN ? = 'GRADED' THEN COALESCE(graded_at, clock_timestamp()) ELSE graded_at END
                WHERE id = ? AND status = ?
                """, submission.status().name(), submission.failureCode(),
                grade == null ? null : write(new StoredGrade(grade.criteria(), grade.corrections(), grade.summary())),
                submission.overallBand(), submission.passed(), submission.ledgerEntryId(),
                Timestamp.from(submission.gradingStartedAt()), submission.status().name(), submission.id(),
                submission.persistedStatus().name()) == 1;
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
            WritingSubmissionStatus status = WritingSubmissionStatus.valueOf(row.getString("status"));
            boolean graded = status == WritingSubmissionStatus.GRADED;
            summaries.put(row.getObject("block_id", UUID.class), new BlockSummary(row.getObject("id", UUID.class),
                    status, graded ? row.getBigDecimal("overall_band") : null,
                    graded ? (Boolean) row.getObject("passed") : null, row.getBoolean("block_passed")));
        });
        return summaries;
    }

    private WritingSubmission submission(ResultSet row, int index) throws SQLException {
        StoredGrade stored = read(row.getString("result"), StoredGrade.class);
        var band = row.getBigDecimal("overall_band");
        return WritingSubmission.restore(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getObject("lesson_id", UUID.class), row.getObject("block_id", UUID.class),
                row.getObject("request_id", UUID.class), row.getString("essay_text"), row.getInt("word_count"),
                read(row.getString("prompt"), EssayPrompt.class), row.getInt("point_cost"),
                WritingSubmissionStatus.valueOf(row.getString("status")), row.getString("failure_code"),
                stored == null ? null : new WritingGrade(stored.criteria(), stored.corrections(), stored.summary(), band),
                (Boolean) row.getObject("passed"), row.getObject("debit_ledger_entry_id", UUID.class),
                row.getTimestamp("grading_started_at").toInstant());
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
