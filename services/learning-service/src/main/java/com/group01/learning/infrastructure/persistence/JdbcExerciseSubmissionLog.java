package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.port.ExerciseSubmissionLog;
import com.group01.learning.application.result.SubmissionResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcExerciseSubmissionLog implements ExerciseSubmissionLog {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcExerciseSubmissionLog(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Stored> find(UUID requestId) {
        return jdbc.query("SELECT user_id, lesson_id, block_id, response FROM lesson_exercise_submissions "
                        + "WHERE request_id = ?", (row, index) -> new Stored(
                        row.getObject("user_id", UUID.class), row.getObject("lesson_id", UUID.class),
                        row.getObject("block_id", UUID.class), readResponse(row.getString("response"))), requestId)
                .stream().findFirst();
    }

    @Override
    public boolean exists(UUID userId, UUID lessonId, UUID blockId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM lesson_exercise_submissions "
                + "WHERE user_id = ? AND lesson_id = ? AND block_id = ?)", Boolean.class, userId, lessonId, blockId));
    }

    @Override
    public boolean save(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command,
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
    public List<SubmissionResult> firstResponses(UUID userId, UUID lessonId) {
        return jdbc.query("""
                SELECT DISTINCT ON (block_id) response FROM lesson_exercise_submissions
                WHERE user_id = ? AND lesson_id = ? ORDER BY block_id, submitted_at, id
                """, (row, index) -> readResponse(row.getString("response")), userId, lessonId);
    }

    @Override
    public Map<UUID, Set<UUID>> wrongQuestions(UUID userId, UUID lessonId) {
        return jdbc.query("""
                SELECT DISTINCT s.block_id, (r->>'questionVersionId')::uuid AS question_version_id
                FROM lesson_exercise_submissions s
                CROSS JOIN LATERAL jsonb_array_elements(s.response->'results') r
                WHERE s.user_id = ? AND s.lesson_id = ? AND r->>'correct' = 'false'
                """, (ResultSetExtractor<Map<UUID, Set<UUID>>>) rows -> {
            Map<UUID, Set<UUID>> wrong = new HashMap<>();
            while (rows.next()) {
                wrong.computeIfAbsent(rows.getObject("block_id", UUID.class), ignored -> new HashSet<>())
                        .add(rows.getObject("question_version_id", UUID.class));
            }
            return wrong;
        }, userId, lessonId);
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
