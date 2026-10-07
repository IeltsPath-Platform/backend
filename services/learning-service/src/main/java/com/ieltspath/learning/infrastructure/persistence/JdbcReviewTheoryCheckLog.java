package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.port.ReviewTheoryCheckLog;
import com.ieltspath.learning.application.result.TheoryCheckResult;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcReviewTheoryCheckLog implements ReviewTheoryCheckLog {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcReviewTheoryCheckLog(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Stored> find(UUID requestId) {
        return jdbc.query("""
                SELECT user_id, review_item_id, response FROM review_theory_checks WHERE request_id = :requestId
                """, Map.of("requestId", requestId), (row, index) -> new Stored(row.getObject("user_id", UUID.class),
                row.getObject("review_item_id", UUID.class), read(row.getString("response")))).stream().findFirst();
    }

    @Override
    public void save(UUID userId, UUID reviewId, List<UUID> questionVersionIds, SubmitExerciseCommand command,
                     TheoryCheckResult response) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", UUID.randomUUID());
        params.put("reviewId", reviewId);
        params.put("userId", userId);
        params.put("requestId", command.requestId());
        params.put("questions", questionVersionIds.stream().map(UUID::toString)
                .collect(Collectors.joining(",", "{", "}")));
        params.put("answers", write(command.answers()));
        params.put("correct", response.correct());
        params.put("total", response.total());
        params.put("response", write(response));
        jdbc.update("""
                INSERT INTO review_theory_checks (id, review_item_id, user_id, request_id, question_version_ids,
                                                  answers, correct_count, total_count, response)
                VALUES (:id, :reviewId, :userId, :requestId, CAST(:questions AS uuid[]), CAST(:answers AS jsonb),
                        :correct, :total, CAST(:response AS jsonb))
                """, params);
    }

    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot store theory check"); }
    }

    private TheoryCheckResult read(String value) {
        try { return json.readValue(value, TheoryCheckResult.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read theory check"); }
    }
}
