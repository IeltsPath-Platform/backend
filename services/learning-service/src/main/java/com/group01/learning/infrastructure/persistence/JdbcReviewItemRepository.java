package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.aggregate.ReviewItem;
import com.group01.learning.domain.entity.ReviewSet;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.ReviewRule.ReviewCandidate;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.vo.ReviewStatus;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public class JdbcReviewItemRepository implements ReviewItemRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcReviewItemRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ReviewItem> findOwned(UUID userId, UUID reviewId) {
        return jdbc.query("""
                SELECT r.id, r.knowledge_point_id, r.lesson_id, r.status,
                s.id AS set_id, s.package_id, s.package_version_id,
                (SELECT count(*) FROM review_sets f WHERE f.review_item_id = r.id AND f.passed = FALSE) AS failed_sets
                FROM review_items r
                LEFT JOIN review_sets s ON s.review_item_id = r.id AND s.submitted_at IS NULL
                WHERE r.id = :reviewId AND r.user_id = :userId
                """, Map.of("reviewId", reviewId, "userId", userId), (row, index) -> {
            UUID setId = row.getObject("set_id", UUID.class);
            ReviewSet open = setId == null ? null : new ReviewSet(setId, row.getObject("package_id", UUID.class),
                    row.getObject("package_version_id", UUID.class));
            return ReviewItem.restore(row.getObject("id", UUID.class), userId,
                    row.getObject("knowledge_point_id", UUID.class), row.getObject("lesson_id", UUID.class),
                    ReviewStatus.valueOf(row.getString("status")), open, row.getInt("failed_sets"));
        }).stream().findFirst();
    }

    @Override
    public List<PendingReview> findPending(UUID userId) {
        return jdbc.query("""
                SELECT id, lesson_id, knowledge_point_id FROM review_items
                WHERE user_id = :userId AND status = 'PENDING' ORDER BY created_at, id
                """, Map.of("userId", userId), (row, index) -> new PendingReview(row.getObject("id", UUID.class),
                row.getObject("lesson_id", UUID.class), row.getObject("knowledge_point_id", UUID.class)));
    }

    @Override
    public void insertPending(UUID userId, List<ReviewCandidate> candidates) {
        if (candidates.isEmpty()) return;
        jdbc.getJdbcOperations().batchUpdate("""
                INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status)
                VALUES (?, ?, ?, ?, 'PENDING')
                ON CONFLICT (user_id, knowledge_point_id) WHERE status = 'PENDING' DO NOTHING
                """, candidates, candidates.size(), (statement, review) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, review.knowledgePointId());
            statement.setObject(4, review.lessonId());
        });
    }

    /** Called once per loaded review: inserts the set it assigned, closes the set it answered, then the status. */
    @Override
    public void save(ReviewItem review) {
        review.assignedSet().ifPresent(set -> jdbc.update("""
                INSERT INTO review_sets (id, review_item_id, user_id, package_id, package_version_id, assigned_at)
                VALUES (:id, :reviewId, :userId, :packageId, :versionId, clock_timestamp())
                """, Map.of("id", set.id(), "reviewId", review.id(), "userId", review.userId(),
                "packageId", set.packageId(), "versionId", set.packageVersionId())));
        review.answeredSet().ifPresent(set -> jdbc.update("""
                UPDATE review_sets SET submitted_at = clock_timestamp(), passed = :passed, request_id = :requestId
                WHERE id = :setId AND submitted_at IS NULL
                """, Map.of("setId", set.id(), "passed", set.passed(), "requestId", set.requestId())));
        if (review.status() != ReviewStatus.PENDING) {
            jdbc.update("""
                    UPDATE review_items SET status = :status, done_at = clock_timestamp()
                    WHERE id = :reviewId AND status = 'PENDING'
                    """, Map.of("reviewId", review.id(), "status", review.status().name()));
        }
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
}
