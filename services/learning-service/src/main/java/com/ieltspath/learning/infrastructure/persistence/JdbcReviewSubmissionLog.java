package com.ieltspath.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.ReviewSubmissionLog;
import com.ieltspath.learning.application.result.ReviewSubmissionResult;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReviewSubmissionLog implements ReviewSubmissionLog {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcReviewSubmissionLog(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<Stored> find(UUID requestId) {
        return jdbc.query("""
                SELECT id, review_item_id, user_id, response FROM review_sets WHERE request_id = :requestId
                """, Map.of("requestId", requestId), (row, index) -> new Stored(
                row.getObject("user_id", UUID.class), row.getObject("review_item_id", UUID.class),
                row.getObject("id", UUID.class), read(row.getString("response")))).stream().findFirst();
    }

    @Override
    public void save(UUID setId, ReviewSubmissionResult response) {
        jdbc.update("UPDATE review_sets SET response = CAST(:response AS jsonb) WHERE id = :setId",
                Map.of("setId", setId, "response", write(response)));
    }

    private String write(ReviewSubmissionResult value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot store review state"); }
    }

    private ReviewSubmissionResult read(String value) {
        try { return json.readValue(value, ReviewSubmissionResult.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read review state"); }
    }
}
