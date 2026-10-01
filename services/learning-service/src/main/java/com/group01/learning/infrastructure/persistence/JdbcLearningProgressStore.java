package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.domain.service.ReviewRule.ReviewCandidate;
import com.group01.learning.domain.vo.*;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcLearningProgressStore implements LearningProgressStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcLearningProgressStore(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void lockUser(UUID userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Learning writes require a transaction");
        }
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, userId.toString());
                statement.execute();
            }
            return null;
        });
    }

    @Override
    public void refreshCurriculum(UUID userId, List<TopicOrder> topics,
                                  List<KnowledgePointCatalogEntry> knowledgePoints) {
        jdbc.update("UPDATE topic_progress SET sequence_order = NULL, updated_at = clock_timestamp() "
                + "WHERE user_id = ? AND sequence_order IS NOT NULL", userId);
        batch("""
                INSERT INTO topic_progress (user_id, topic_id, sequence_order) VALUES (?, ?, ?)
                ON CONFLICT (user_id, topic_id) DO UPDATE
                SET sequence_order = EXCLUDED.sequence_order, updated_at = clock_timestamp()
                """, topics, (statement, topic) -> {
            statement.setObject(1, userId);
            statement.setObject(2, topic.topicId());
            statement.setInt(3, topic.sequenceOrder());
        });
        // Shared catalog rows need the same lock order even when Content reorders topics.
        var orderedKnowledgePoints = knowledgePoints.stream()
                .sorted(Comparator.comparing(kp -> kp.knowledgePointId().toString())).toList();
        batch("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set) VALUES (?, ?, ?)
                ON CONFLICT (kp_id) DO UPDATE SET topic_id = EXCLUDED.topic_id,
                has_practice_set = EXCLUDED.has_practice_set, refreshed_at = clock_timestamp()
                """, orderedKnowledgePoints, (statement, kp) -> {
            statement.setObject(1, kp.knowledgePointId());
            statement.setObject(2, kp.topicId());
            statement.setBoolean(3, kp.hasPracticeSet());
        });
    }

    @Override
    public List<TopicProgress> findTopics(UUID userId) {
        return jdbc.query("SELECT topic_id, sequence_order, passed_at FROM topic_progress WHERE user_id = ?",
                (row, index) -> new TopicProgress(row.getObject("topic_id", UUID.class),
                        row.getObject("sequence_order", Integer.class), instant(row, "passed_at")), userId);
    }

    @Override
    public Map<UUID, Integer> completedLessonCounts(UUID userId) {
        Map<UUID, Integer> counts = new HashMap<>();
        jdbc.query("SELECT topic_id, count(*) AS total FROM lesson_progress "
                        + "WHERE user_id = ? AND completed_at IS NOT NULL GROUP BY topic_id",
                (org.springframework.jdbc.core.RowCallbackHandler) row -> counts.put(
                        row.getObject("topic_id", UUID.class), row.getInt("total")), userId);
        return counts;
    }

    @Override
    public Map<UUID, LessonProgress> findLessons(UUID userId, UUID topicId) {
        Map<UUID, LessonProgress> lessons = new HashMap<>();
        jdbc.query("SELECT * FROM lesson_progress WHERE user_id = ? AND topic_id = ?",
                (org.springframework.jdbc.core.RowCallbackHandler) row -> {
                    LessonProgress lesson = lesson(row);
                    lessons.put(lesson.lessonId(), lesson);
                }, userId, topicId);
        return lessons;
    }

    @Override
    public List<LessonProgress> findCompletedLessons(UUID userId) {
        return jdbc.query("SELECT * FROM lesson_progress WHERE user_id = ? AND completed_at IS NOT NULL",
                (row, index) -> lesson(row), userId);
    }

    @Override
    public List<PendingReview> findPendingReviews(UUID userId) {
        return jdbc.query("SELECT id, lesson_id, knowledge_point_id FROM review_items "
                        + "WHERE user_id = ? AND status = 'PENDING' ORDER BY created_at, id",
                (row, index) -> new PendingReview(row.getObject("id", UUID.class),
                        row.getObject("lesson_id", UUID.class), row.getObject("knowledge_point_id", UUID.class)), userId);
    }

    @Override
    public void refreshLesson(UUID userId, UUID lessonId, UUID topicId, int sortOrder, List<UUID> kpIds) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids)
                    VALUES (?, ?, ?, ?, ?) ON CONFLICT (user_id, lesson_id) DO UPDATE
                    SET topic_id = EXCLUDED.topic_id, lesson_sort_order = EXCLUDED.lesson_sort_order,
                    knowledge_point_ids = EXCLUDED.knowledge_point_ids, updated_at = clock_timestamp()
                    """);
            statement.setObject(1, userId);
            statement.setObject(2, lessonId);
            statement.setObject(3, topicId);
            statement.setInt(4, sortOrder);
            statement.setArray(5, connection.createArrayOf("uuid", kpIds.toArray()));
            return statement;
        });
    }

    @Override
    public Optional<StoredSubmission> findSubmission(UUID requestId) {
        return jdbc.query("SELECT user_id, lesson_id, block_id, response FROM lesson_exercise_submissions "
                        + "WHERE request_id = ?", (row, index) -> new StoredSubmission(
                        row.getObject("user_id", UUID.class), row.getObject("lesson_id", UUID.class),
                        row.getObject("block_id", UUID.class), readResponse(row.getString("response"))), requestId)
                .stream().findFirst();
    }

    @Override
    public boolean hasSubmission(UUID userId, UUID lessonId, UUID blockId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM lesson_exercise_submissions "
                + "WHERE user_id = ? AND lesson_id = ? AND block_id = ?)", Boolean.class, userId, lessonId, blockId));
    }

    @Override
    public List<SubmissionResult> findFirstSubmissions(UUID userId, UUID lessonId) {
        return jdbc.query("""
                SELECT DISTINCT ON (block_id) response FROM lesson_exercise_submissions
                WHERE user_id = ? AND lesson_id = ? ORDER BY block_id, submitted_at, id
                """, (row, index) -> readResponse(row.getString("response")), userId, lessonId);
    }

    @Override
    public boolean passBlock(UUID userId, UUID lessonId, UUID blockId) {
        return jdbc.update("""
                UPDATE lesson_progress SET passed_block_ids = array_append(passed_block_ids, ?),
                updated_at = clock_timestamp() WHERE user_id = ? AND lesson_id = ?
                AND NOT (? = ANY(passed_block_ids))
                """, blockId.toString(), userId, lessonId, blockId.toString()) == 1;
    }

    @Override
    public boolean completeLesson(UUID userId, UUID lessonId) {
        return jdbc.update("UPDATE lesson_progress SET completed_at = clock_timestamp(), updated_at = clock_timestamp() "
                + "WHERE user_id = ? AND lesson_id = ? AND completed_at IS NULL", userId, lessonId) == 1;
    }

    @Override
    public void appendEvidence(UUID userId, List<NewEvidence> evidence) {
        batch("""
                INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (user_id, source, source_reference_id) DO NOTHING
                """, evidence, (statement, item) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, item.knowledgePointId());
            statement.setBoolean(4, item.correct());
            statement.setString(5, item.source());
            statement.setObject(6, item.sourceReferenceId());
        });
    }

    @Override
    public boolean saveSubmission(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command,
                                  SubmissionResult response) {
        // Capture insertion time after the user lock, rather than the transaction's start time.
        return jdbc.update("""
                INSERT INTO lesson_exercise_submissions
                (id, user_id, lesson_id, block_id, request_id, answers, block_passed, response, submitted_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, CAST(? AS jsonb), clock_timestamp())
                ON CONFLICT (request_id) DO NOTHING
                """, UUID.randomUUID(), userId, lessonId, blockId, command.requestId(), writeJson(command.answers()),
                response.blockPassed(), writeJson(response)) == 1;
    }

    @Override
    public List<MasteryHistory> findMastery(UUID userId) {
        return jdbc.query("""
                WITH history AS (
                    SELECT kp_id, correct, ordinal, count(*) OVER (PARTITION BY kp_id) AS evidence_count,
                    row_number() OVER (PARTITION BY kp_id ORDER BY ordinal DESC) AS recency
                    FROM kp_evidence WHERE user_id = ?
                )
                SELECT catalog.kp_id, catalog.topic_id, catalog.has_practice_set,
                history.correct, history.ordinal, COALESCE(history.evidence_count, 0) AS evidence_count
                FROM knowledge_point_catalog catalog LEFT JOIN history
                ON history.kp_id = catalog.kp_id AND history.recency <= 5
                ORDER BY catalog.topic_id, catalog.kp_id, history.ordinal
                """, (org.springframework.jdbc.core.ResultSetExtractor<List<MasteryHistory>>) rows -> {
            Map<UUID, MasteryHistory> histories = new LinkedHashMap<>();
            while (rows.next()) {
                UUID kpId = rows.getObject("kp_id", UUID.class);
                MasteryHistory history = histories.get(kpId);
                if (history == null) {
                    history = new MasteryHistory(kpId, rows.getObject("topic_id", UUID.class),
                            rows.getBoolean("has_practice_set"), new ArrayList<>(), rows.getLong("evidence_count"));
                    histories.put(kpId, history);
                }
                if (rows.getObject("ordinal") != null) history.correctness().add(rows.getBoolean("correct"));
            }
            return List.copyOf(histories.values());
        }, userId);
    }

    @Override
    public void insertReviews(UUID userId, List<ReviewCandidate> reviews) {
        batch("""
                INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status)
                VALUES (?, ?, ?, ?, 'PENDING')
                ON CONFLICT (user_id, knowledge_point_id) WHERE status = 'PENDING' DO NOTHING
                """, reviews, (statement, review) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, review.knowledgePointId());
            statement.setObject(4, review.lessonId());
        });
    }

    private LessonProgress lesson(ResultSet row) throws SQLException {
        return new LessonProgress(row.getObject("lesson_id", UUID.class), row.getObject("topic_id", UUID.class),
                row.getInt("lesson_sort_order"), uuids(row.getArray("knowledge_point_ids")),
                new HashSet<>(uuids(row.getArray("passed_block_ids"))), instant(row, "completed_at"));
    }

    private List<UUID> uuids(Array array) throws SQLException {
        try {
            return Arrays.stream((Object[]) array.getArray()).map(value -> UUID.fromString(value.toString())).toList();
        } finally {
            array.free();
        }
    }

    private Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp timestamp = row.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private <T> void batch(String sql, List<T> values, ParameterizedPreparedStatementSetter<T> setter) {
        if (!values.isEmpty()) jdbc.batchUpdate(sql, values, values.size(), setter);
    }

    private String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot store learning state"); }
    }

    private SubmissionResult readResponse(String value) {
        try { return json.readValue(value, SubmissionResult.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read learning state"); }
    }
}
