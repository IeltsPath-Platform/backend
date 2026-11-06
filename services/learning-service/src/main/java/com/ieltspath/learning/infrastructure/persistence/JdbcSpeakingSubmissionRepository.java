package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingRecording;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSpeakingSubmissionRepository implements SpeakingSubmissionRepository {
    private static final String SUBMISSION_COLUMNS = """
            id, user_id, request_id, package_version_id, scope, status, point_cost, consent_at, failure_code,
            created_at, updated_at, queued_at, result::text AS result_json, overall_band, debit_ledger_entry_id,
            rubric_code, rubric_version, prompt_checksum, prompt_version, grading_attempts, grading_started_at,
            graded_at, rubric_snapshot::text AS rubric_snapshot_json""";
    private static final String RECORDING_COLUMNS = """
            submission_id, question_version_id, section_id, part_no, question_no, attempt, object_key, content_type,
            size_bytes, duration_ms, uploaded_at, question_text""";

    private final JdbcTemplate jdbc;
    private static final ObjectMapper JSON = new ObjectMapper();

    public JdbcSpeakingSubmissionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SpeakingSubmission> findByRequestId(UUID requestId) {
        return load("WHERE request_id = ?", requestId).stream().findFirst();
    }

    @Override
    public Optional<SpeakingSubmission> findById(UUID id) {
        return load("WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public Optional<SpeakingSubmission> findOwned(UUID id, UUID userId) {
        return load("WHERE id = ? AND user_id = ?", id, userId).stream().findFirst();
    }

    @Override
    public Optional<SpeakingSubmission> findForUpdate(UUID id) {
        return load("WHERE id = ? FOR UPDATE", id).stream().findFirst();
    }

    @Override
    public List<SpeakingSubmission> findActive(UUID userId) {
        return load("WHERE user_id = ? AND status IN ('UPLOADING', 'QUEUED', 'ANALYZING', 'GRADING', 'PAYMENT_PENDING', 'FAILED_RETRYABLE') "
                + "ORDER BY created_at", userId);
    }

    @Override
    public Optional<SpeakingSubmission> claimNext(Instant staleBefore, int maxAttempts) {
        Timestamp cutoff = Timestamp.from(staleBefore);
        jdbc.update("UPDATE speaking_submissions SET status='NEEDS_EXAMINER', failure_code='MAX_ATTEMPTS_REACHED', "
                        + "updated_at=CURRENT_TIMESTAMP WHERE status IN ('QUEUED','FAILED_RETRYABLE','ANALYZING','GRADING') "
                        + "AND grading_attempts >= ? AND (status = 'QUEUED' OR updated_at < ?)",
                maxAttempts, cutoff);
        List<UUID> ids = jdbc.query("""
                WITH candidate AS (
                    SELECT id FROM speaking_submissions
                    WHERE (status = 'QUEUED' OR (status = 'FAILED_RETRYABLE' AND updated_at < ?)
                           OR (status IN ('ANALYZING', 'GRADING') AND updated_at < ?))
                    ORDER BY queued_at NULLS LAST, created_at
                    FOR UPDATE SKIP LOCKED LIMIT 1
                )
                UPDATE speaking_submissions s SET
                    status = 'ANALYZING',
                    failure_code = NULL,
                    grading_attempts = s.grading_attempts + 1,
                    grading_started_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                FROM candidate WHERE s.id = candidate.id
                RETURNING s.id
                """, (rs, row) -> rs.getObject(1, UUID.class), cutoff, cutoff);
        return ids.stream().findFirst().flatMap(this::findById);
    }

    @Override
    public void touch(UUID id) {
        jdbc.update("UPDATE speaking_submissions SET updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status IN ('ANALYZING', 'GRADING')", id);
    }

    @Override
    public Optional<String> findGradingContext(UUID submissionId) {
        return jdbc.query("SELECT context::text FROM speaking_grading_contexts WHERE submission_id = ?",
                (rs, row) -> rs.getString(1), submissionId).stream().findFirst();
    }

    @Override
    public void saveGradingContext(UUID submissionId, String contextJson) {
        jdbc.update("""
                INSERT INTO speaking_grading_contexts (submission_id, context) VALUES (?, CAST(? AS JSONB))
                ON CONFLICT (submission_id) DO NOTHING
                """, submissionId, contextJson);
    }

    @Override
    public Optional<String> findAnalysis(UUID submissionId, UUID questionVersionId) {
        return jdbc.query("SELECT result::text FROM speaking_analyses WHERE submission_id=? AND question_version_id=? "
                        + "AND status='ANALYZED'", (rs, row) -> rs.getString(1), submissionId, questionVersionId)
                .stream().findFirst();
    }

    @Override
    public void saveAnalysis(UUID submissionId, UUID questionVersionId, String analysisJson) {
        jdbc.update("""
                INSERT INTO speaking_analyses (submission_id, question_version_id, status, result, updated_at)
                VALUES (?, ?, 'ANALYZED', CAST(? AS JSONB), CURRENT_TIMESTAMP)
                ON CONFLICT (submission_id, question_version_id) DO UPDATE
                    SET status='ANALYZED', result=EXCLUDED.result, updated_at=CURRENT_TIMESTAMP
                """, submissionId, questionVersionId, analysisJson);
    }

    @Override
    public List<SpeakingSubmission> findExpiredBefore(Instant cutoff, int limit) {
        return load("WHERE created_at < ? ORDER BY created_at LIMIT ?", Timestamp.from(cutoff), limit);
    }

    @Override
    public boolean deleteExpired(UUID id, Instant cutoff) {
        return jdbc.update("DELETE FROM speaking_submissions WHERE id = ? AND created_at < ?", id,
                Timestamp.from(cutoff)) == 1;
    }

    @Override
    public void save(SpeakingSubmission submission) {
        if (submission.isNew()) {
            insert(submission);
            submission.persisted();
        } else {
            update(submission);
        }
    }

    /** One query for the submissions and one for all their recordings; the filter is the same for both. */
    private List<SpeakingSubmission> load(String where, Object... args) {
        List<SubmissionRow> rows = jdbc.query("SELECT " + SUBMISSION_COLUMNS + " FROM speaking_submissions " + where,
                (resultSet, row) -> submissionRow(resultSet), args);
        if (rows.isEmpty()) return List.of();
        Map<UUID, List<SpeakingRecording>> recordings = new LinkedHashMap<>();
        for (SubmissionRow row : rows) recordings.put(row.id(), new ArrayList<>());
        String placeholders = String.join(", ", java.util.Collections.nCopies(rows.size(), "?"));
        jdbc.query("SELECT " + RECORDING_COLUMNS + " FROM speaking_recordings WHERE submission_id IN (" + placeholders + ")",
                resultSet -> {
                    recordings.get(resultSet.getObject("submission_id", UUID.class)).add(recording(resultSet));
                },
                rows.stream().map(SubmissionRow::id).toArray());
        List<SpeakingSubmission> submissions = new ArrayList<>();
        for (SubmissionRow row : rows) submissions.add(row.toAggregate(recordings.get(row.id())));
        return submissions;
    }

    private void insert(SpeakingSubmission submission) {
        jdbc.update("""
                INSERT INTO speaking_submissions (id, user_id, request_id, package_version_id, scope, status,
                    point_cost, consent_at, failure_code, created_at, updated_at, queued_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, submission.id(), submission.userId(), submission.requestId(), submission.packageVersionId(),
                submission.scope().name(), submission.status().name(), submission.pointCost(),
                Timestamp.from(submission.consentAt()), submission.failureCode(),
                Timestamp.from(submission.createdAt()), Timestamp.from(submission.updatedAt()),
                timestamp(submission.queuedAt()));
        jdbc.batchUpdate("""
                INSERT INTO speaking_recordings (submission_id, question_version_id, section_id, part_no, question_no,
                    attempt, object_key, content_type, size_bytes, duration_ms, uploaded_at, question_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, submission.recordings(), submission.recordings().size(), (statement, recording) -> {
            statement.setObject(1, submission.id());
            statement.setObject(2, recording.questionVersionId());
            statement.setObject(3, recording.sectionId());
            statement.setInt(4, recording.partNo());
            statement.setInt(5, recording.questionNo());
            statement.setInt(6, recording.attempt());
            statement.setString(7, recording.objectKey());
            statement.setString(8, recording.contentType());
            statement.setObject(9, recording.isUploaded() ? recording.sizeBytes() : null);
            statement.setObject(10, recording.durationMs());
            statement.setTimestamp(11, timestamp(recording.uploadedAt()));
            statement.setString(12, recording.questionText());
        });
    }

    private void update(SpeakingSubmission submission) {
        jdbc.update("""
                UPDATE speaking_submissions SET status = ?, failure_code = ?, updated_at = ?, queued_at = ?,
                    result = CAST(? AS JSONB), overall_band = ?, debit_ledger_entry_id = ?, rubric_code = ?,
                    rubric_version = ?, prompt_checksum = ?, prompt_version = ?, grading_attempts = ?,
                    grading_started_at = ?, graded_at = ?, rubric_snapshot = CAST(? AS JSONB)
                WHERE id = ?
                """, submission.status().name(), submission.failureCode(), Timestamp.from(submission.updatedAt()),
                timestamp(submission.queuedAt()), json(submission.result()), submission.overallBand(),
                submission.debitLedgerEntryId(), submission.rubricCode(), submission.rubricVersion(),
                submission.promptChecksum(), submission.promptVersion(), submission.gradingAttempts(),
                timestamp(submission.gradingStartedAt()), timestamp(submission.gradedAt()),
                json(submission.rubricSnapshot()), submission.id());
        jdbc.batchUpdate("""
                UPDATE speaking_recordings SET attempt = ?, object_key = ?, content_type = ?, size_bytes = ?,
                    duration_ms = ?, uploaded_at = ?
                WHERE submission_id = ? AND question_version_id = ?
                """, submission.recordings(), submission.recordings().size(), (statement, recording) -> {
            statement.setInt(1, recording.attempt());
            statement.setString(2, recording.objectKey());
            statement.setString(3, recording.contentType());
            statement.setObject(4, recording.isUploaded() ? recording.sizeBytes() : null);
            statement.setObject(5, recording.durationMs());
            statement.setTimestamp(6, timestamp(recording.uploadedAt()));
            statement.setObject(7, submission.id());
            statement.setObject(8, recording.questionVersionId());
        });
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static String json(Map<String, Object> value) {
        if (value == null) return null;
        try { return JSON.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalArgumentException("Speaking result is not serializable", exception); }
    }

    private static Map<String, Object> readJson(String value) {
        if (value == null) return null;
        try { return JSON.readValue(value, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalStateException("Stored Speaking result is invalid", exception); }
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static SubmissionRow submissionRow(ResultSet resultSet) throws SQLException {
        String resultJson = resultSet.getString("result_json");
        return new SubmissionRow(resultSet.getObject("id", UUID.class), resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("request_id", UUID.class), resultSet.getObject("package_version_id", UUID.class),
                SpeakingScope.valueOf(resultSet.getString("scope")),
                SpeakingSubmissionStatus.valueOf(resultSet.getString("status")), resultSet.getInt("point_cost"),
                instant(resultSet, "consent_at"), resultSet.getString("failure_code"),
                instant(resultSet, "created_at"), instant(resultSet, "updated_at"), instant(resultSet, "queued_at"),
                readJson(resultJson), resultSet.getBigDecimal("overall_band"),
                resultSet.getObject("debit_ledger_entry_id", UUID.class), resultSet.getString("rubric_code"),
                (Integer) resultSet.getObject("rubric_version"), resultSet.getString("prompt_checksum"),
                resultSet.getString("prompt_version"), resultSet.getInt("grading_attempts"),
                instant(resultSet, "grading_started_at"), instant(resultSet, "graded_at"),
                readJson(resultSet.getString("rubric_snapshot_json")));
    }

    private static SpeakingRecording recording(ResultSet resultSet) throws SQLException {
        // wasNull() answers for the column read just before it, so the duration is read last-but-asked-first.
        int duration = resultSet.getInt("duration_ms");
        Integer durationMs = resultSet.wasNull() ? null : duration;
        return new SpeakingRecording(resultSet.getObject("question_version_id", UUID.class),
                resultSet.getObject("section_id", UUID.class), resultSet.getInt("part_no"),
                resultSet.getInt("question_no"), resultSet.getInt("attempt"), resultSet.getString("object_key"),
                resultSet.getString("content_type"), resultSet.getLong("size_bytes"),
                durationMs, instant(resultSet, "uploaded_at"), resultSet.getString("question_text"));
    }

    private record SubmissionRow(UUID id, UUID userId, UUID requestId, UUID packageVersionId, SpeakingScope scope,
                                 SpeakingSubmissionStatus status, int pointCost, Instant consentAt,
                                 String failureCode, Instant createdAt, Instant updatedAt, Instant queuedAt,
                                 Map<String, Object> result, BigDecimal overallBand, UUID debitLedgerEntryId,
                                 String rubricCode, Integer rubricVersion, String promptChecksum, String promptVersion,
                                 int gradingAttempts, Instant gradingStartedAt, Instant gradedAt,
                                 Map<String, Object> rubricSnapshot) {
        SpeakingSubmission toAggregate(List<SpeakingRecording> recordings) {
            return SpeakingSubmission.restore(id, userId, requestId, packageVersionId, scope, status, pointCost,
                    consentAt, failureCode, createdAt, updatedAt, queuedAt, recordings, result, overallBand,
                    debitLedgerEntryId, rubricCode, rubricVersion, promptChecksum, promptVersion, gradingAttempts,
                    gradingStartedAt, gradedAt, rubricSnapshot);
        }
    }
}
