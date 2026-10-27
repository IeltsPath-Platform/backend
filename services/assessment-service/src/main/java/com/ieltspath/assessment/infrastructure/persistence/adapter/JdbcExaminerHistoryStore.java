package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.ExaminerHistoryStore;
import com.ieltspath.assessment.application.port.PromptImage;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcExaminerHistoryStore implements ExaminerHistoryStore {
    /** The examiner's own finished human jobs that have a structured review (band, criteria, marked errors). */
    private static final String FINISHED = """
            FROM grading_jobs j JOIN human_review_submissions h ON h.id=j.human_review_submission_id
            WHERE j.grading_mode='HUMAN' AND j.status='COMPLETED' AND COALESCE(j.reason,'')<>'QA_SAMPLE'
              AND j.assigned_examiner_id=:examiner AND h.band IS NOT NULL
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcExaminerHistoryStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional(readOnly = true)
    public Page completed(UUID examinerId, int page, int size) {
        var args = new MapSqlParameterSource("examiner", examinerId).addValue("limit", size).addValue("offset", (long) page * size);
        List<Item> rows = jdbc.query("SELECT j.id,h.source,j.skill,h.task,h.band,j.completed_at " + FINISHED
                + " ORDER BY j.completed_at DESC NULLS LAST,j.id LIMIT :limit OFFSET :offset", args,
                (row, index) -> new Item(row.getObject("id", UUID.class), row.getString("source"), row.getString("skill"),
                        row.getString("task"), row.getBigDecimal("band"),
                        row.getTimestamp("completed_at") == null ? null : row.getTimestamp("completed_at").toInstant()));
        Long total = jdbc.queryForObject("SELECT count(*) " + FINISHED, args, Long.class);
        return new Page(rows, page, size, total == null ? 0 : total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GradedReview> review(UUID examinerId, UUID jobId) {
        return jdbc.query("""
                SELECT j.id,h.source,j.skill,h.task,h.prompt_snapshot->>'stem' AS prompt,h.prompt_snapshot->'images' AS images,h.essay_text,h.band,
                       h.criteria::text AS criteria,h.error_codes,h.feedback,h.annotations::text AS annotations,j.completed_at
                """ + FINISHED + " AND j.id=:id", new MapSqlParameterSource("examiner", examinerId).addValue("id", jobId),
                (row, index) -> {
                    try {
                        String marked = row.getString("annotations");
                        String charts = row.getString("images");
                        return new GradedReview(row.getObject("id", UUID.class), row.getString("source"),
                                row.getString("skill"), row.getString("task"), row.getString("prompt"),
                                charts == null ? List.of() : PromptImage.listOf(json.readTree(charts)),
                                row.getString("essay_text"), row.getBigDecimal("band"),
                                json.readTree(row.getString("criteria")), codes(row.getArray("error_codes")),
                                row.getString("feedback"), marked == null ? null : json.readTree(marked),
                                row.getTimestamp("completed_at") == null ? null : row.getTimestamp("completed_at").toInstant());
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException("Stored human review is invalid", exception);
                    }
                }).stream().findFirst();
    }

    private static List<String> codes(Array array) throws SQLException {
        if (array == null) return List.of();
        try {
            return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }
}
