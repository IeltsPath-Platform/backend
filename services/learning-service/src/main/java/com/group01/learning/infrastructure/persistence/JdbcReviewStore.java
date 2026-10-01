package com.group01.learning.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.learning.application.port.ReviewStore;
import com.group01.learning.application.result.ReviewSubmissionResult;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReviewStore implements ReviewStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcReviewStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Optional<ReviewItem> findReview(UUID userId, UUID reviewId) {
        return jdbc.query("""
                SELECT id, lesson_id, knowledge_point_id, status FROM review_items
                WHERE id = :reviewId AND user_id = :userId
                """, Map.of("reviewId", reviewId, "userId", userId), (row, index) -> new ReviewItem(
                row.getObject("id", UUID.class), row.getObject("lesson_id", UUID.class),
                row.getObject("knowledge_point_id", UUID.class), row.getString("status"))).stream().findFirst();
    }

    @Override
    public Optional<ReviewSet> findOpenSet(UUID reviewId) {
        return jdbc.query("""
                SELECT id, review_item_id, package_id, package_version_id FROM review_sets
                WHERE review_item_id = :reviewId AND submitted_at IS NULL
                """, Map.of("reviewId", reviewId), (row, index) -> reviewSet(row)).stream().findFirst();
    }

    @Override
    public Optional<ReviewSet> lockOpenSet(UUID userId, UUID reviewId, UUID setId) {
        return jdbc.query("""
                SELECT id, review_item_id, package_id, package_version_id FROM review_sets
                WHERE id = :setId AND review_item_id = :reviewId AND user_id = :userId AND submitted_at IS NULL
                FOR UPDATE
                """, Map.of("setId", setId, "reviewId", reviewId, "userId", userId),
                (row, index) -> reviewSet(row)).stream().findFirst();
    }

    @Override
    public List<UUID> assignedPackageIds(UUID userId) {
        return jdbc.queryForList("SELECT DISTINCT package_id FROM review_sets WHERE user_id = :userId",
                Map.of("userId", userId), UUID.class);
    }

    @Override
    public Map<UUID, Instant> lastAssignedAt(UUID userId, Collection<UUID> packageIds) {
        Map<UUID, Instant> latest = new HashMap<>();
        if (packageIds.isEmpty()) return latest;
        jdbc.query("""
                SELECT package_id, max(assigned_at) AS last_assigned FROM review_sets
                WHERE user_id = :userId AND package_id IN (:packageIds) GROUP BY package_id
                """, Map.of("userId", userId, "packageIds", packageIds), (RowCallbackHandler) row -> latest.put(
                row.getObject("package_id", UUID.class), row.getTimestamp("last_assigned").toInstant()));
        return latest;
    }

    @Override
    public UUID insertSet(UUID userId, UUID reviewId, UUID packageId, UUID packageVersionId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, assigned_at)
                VALUES (:id, :reviewId, :userId, :packageId, :versionId, clock_timestamp())
                """, Map.of("id", id, "reviewId", reviewId, "userId", userId, "packageId", packageId,
                "versionId", packageVersionId));
        return id;
    }

    @Override
    public Optional<StoredReviewSubmission> findSubmission(UUID requestId) {
        return jdbc.query("""
                SELECT s.id, s.review_item_id, s.user_id, s.response FROM review_sets s
                WHERE s.request_id = :requestId
                """, Map.of("requestId", requestId), (row, index) -> new StoredReviewSubmission(
                row.getObject("user_id", UUID.class), row.getObject("review_item_id", UUID.class),
                row.getObject("id", UUID.class), read(row.getString("response")))).stream().findFirst();
    }

    @Override
    public void closeSet(UUID setId, UUID requestId, boolean passed, ReviewSubmissionResult response) {
        jdbc.update("""
                UPDATE review_sets SET submitted_at = clock_timestamp(), passed = :passed, request_id = :requestId,
                response = CAST(:response AS jsonb)
                WHERE id = :setId AND submitted_at IS NULL
                """, Map.of("setId", setId, "requestId", requestId, "passed", passed, "response", write(response)));
    }

    @Override
    public int failedSetCount(UUID reviewId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM review_sets WHERE review_item_id = :reviewId AND passed = FALSE",
                Map.of("reviewId", reviewId), Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public void finishReview(UUID reviewId, String status) {
        jdbc.update("""
                UPDATE review_items SET status = :status, done_at = clock_timestamp()
                WHERE id = :reviewId AND status = 'PENDING'
                """, Map.of("reviewId", reviewId, "status", status));
    }

    @Override
    public Optional<TestAssignment> findOpenAssignment(UUID userId, UUID topicId) {
        return jdbc.query("""
                SELECT id, package_id, package_version_id FROM topic_test_assignments
                WHERE user_id = :userId AND topic_id = :topicId AND consumed_at IS NULL
                """, Map.of("userId", userId, "topicId", topicId), (row, index) -> new TestAssignment(
                row.getObject("id", UUID.class), row.getObject("package_id", UUID.class),
                row.getObject("package_version_id", UUID.class))).stream().findFirst();
    }

    @Override
    public Map<UUID, Instant> lastConsumedAt(UUID userId, UUID topicId) {
        Map<UUID, Instant> latest = new HashMap<>();
        jdbc.query("""
                SELECT package_id, max(consumed_at) AS last_consumed FROM topic_test_assignments
                WHERE user_id = :userId AND topic_id = :topicId AND consumed_at IS NOT NULL GROUP BY package_id
                """, Map.of("userId", userId, "topicId", topicId), (RowCallbackHandler) row -> latest.put(
                row.getObject("package_id", UUID.class), row.getTimestamp("last_consumed").toInstant()));
        return latest;
    }

    @Override
    public TestAssignment insertAssignment(UUID userId, UUID topicId, UUID packageId, UUID packageVersionId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO topic_test_assignments (id, user_id, topic_id, package_id, package_version_id, assigned_at)
                VALUES (:id, :userId, :topicId, :packageId, :versionId, clock_timestamp())
                """, Map.of("id", id, "userId", userId, "topicId", topicId, "packageId", packageId,
                "versionId", packageVersionId));
        return new TestAssignment(id, packageId, packageVersionId);
    }

    private static ReviewSet reviewSet(ResultSet row) throws SQLException {
        return new ReviewSet(row.getObject("id", UUID.class), row.getObject("review_item_id", UUID.class),
                row.getObject("package_id", UUID.class), row.getObject("package_version_id", UUID.class));
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
