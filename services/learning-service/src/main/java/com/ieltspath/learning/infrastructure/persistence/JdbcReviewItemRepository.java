package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.entity.ReviewSet;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.PracticeReviewState;
import com.ieltspath.learning.domain.vo.ReviewListEntry;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TheoryReason;
import com.ieltspath.learning.domain.vo.TopicReviewSnapshot;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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
                SELECT r.id, r.knowledge_point_id, r.lesson_id, r.status, r.skill, r.stage, r.theory_reason,
                r.theory_completed_count, r.trigger_kind, s.id AS set_id, s.package_id, s.package_version_id,
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
                    ReviewStatus.valueOf(row.getString("status")), open, row.getInt("failed_sets"),
                    row.getString("skill") == null ? null : LearningSkill.valueOf(row.getString("skill")),
                    ReviewStage.valueOf(row.getString("stage")),
                    row.getString("theory_reason") == null ? null : TheoryReason.valueOf(row.getString("theory_reason")),
                    row.getInt("theory_completed_count"), row.getString("trigger_kind"));
        }).stream().findFirst();
    }

    @Override
    public List<PendingReview> findPending(UUID userId) {
        return jdbc.query("""
                SELECT id, lesson_id, knowledge_point_id, skill FROM review_items
                WHERE user_id = :userId AND status = 'PENDING' ORDER BY created_at, id
                """, Map.of("userId", userId), (row, index) -> new PendingReview(row.getObject("id", UUID.class),
                row.getObject("lesson_id", UUID.class), row.getObject("knowledge_point_id", UUID.class),
                row.getString("skill") == null ? null : LearningSkill.valueOf(row.getString("skill"))));
    }

    @Override
    public List<PracticeReviewState> findPracticeByLessons(UUID userId, Collection<UUID> lessonIds) {
        if (lessonIds.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT lesson_id, knowledge_point_id, status, skill FROM review_items
                WHERE user_id = :userId AND lesson_id IN (:lessonIds) AND trigger_kind = 'PRACTICE'
                """, Map.of("userId", userId, "lessonIds", lessonIds), (row, index) ->
                new PracticeReviewState(row.getObject("lesson_id", UUID.class),
                        row.getObject("knowledge_point_id", UUID.class),
                        ReviewStatus.valueOf(row.getString("status")), skill(row.getString("skill"))));
    }

    @Override
    public TopicReviewSnapshot findForTopic(UUID userId, Collection<UUID> lessonIds) {
        List<PendingReview> pending = new ArrayList<>();
        List<PracticeReviewState> practice = new ArrayList<>();
        if (lessonIds.isEmpty()) return new TopicReviewSnapshot(findPending(userId), List.of());
        jdbc.query("""
                SELECT id, lesson_id, knowledge_point_id, status, skill, trigger_kind
                FROM review_items
                WHERE user_id = :userId
                  AND (status = 'PENDING' OR (lesson_id IN (:lessonIds) AND trigger_kind = 'PRACTICE'))
                ORDER BY created_at, id
                """, Map.of("userId", userId, "lessonIds", lessonIds), (RowCallbackHandler) row -> {
            UUID lessonId = row.getObject("lesson_id", UUID.class);
            UUID kpId = row.getObject("knowledge_point_id", UUID.class);
            ReviewStatus status = ReviewStatus.valueOf(row.getString("status"));
            LearningSkill skill = skill(row.getString("skill"));
            if (status == ReviewStatus.PENDING) pending.add(new PendingReview(row.getObject("id", UUID.class),
                    lessonId, kpId, skill));
            if ("PRACTICE".equals(row.getString("trigger_kind")) && lessonIds.contains(lessonId)) {
                practice.add(new PracticeReviewState(lessonId, kpId, status, skill));
            }
        });
        return new TopicReviewSnapshot(List.copyOf(pending), List.copyOf(practice));
    }

    private static LearningSkill skill(String value) {
        return value == null ? null : LearningSkill.valueOf(value);
    }

    @Override
    public void insertPracticePending(UUID userId, List<PracticeReviewCandidate> candidates) {
        if (candidates.isEmpty()) return;
        jdbc.getJdbcOperations().batchUpdate("""
                INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill,
                                          trigger_kind, source_attempt_id, stage, theory_reason)
                VALUES (?, ?, ?, ?, 'PENDING', ?, 'PRACTICE', ?, ?, ?)
                ON CONFLICT (user_id, knowledge_point_id) WHERE status = 'PENDING' DO NOTHING
                """, candidates, candidates.size(), (statement, review) -> {
            statement.setObject(1, review.reviewId());
            statement.setObject(2, userId);
            statement.setObject(3, review.knowledgePointId());
            statement.setObject(4, review.lessonId());
            statement.setString(5, review.skill().name());
            statement.setObject(6, review.sourceAttemptId());
            statement.setString(7, review.stage().name());
            statement.setString(8, review.theoryReason() == null ? null : review.theoryReason().name());
        });
    }

    @Override
    public List<ReviewListEntry> list(UUID userId, ReviewStatus status, LearningSkill skill, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("status", status.name());
        params.put("skill", skill == null ? null : skill.name());
        params.put("limit", limit);
        return jdbc.query("""
                SELECT id, knowledge_point_id, lesson_id, skill, stage, created_at FROM review_items
                WHERE user_id = :userId AND status = :status
                  AND (CAST(:skill AS varchar) IS NULL OR skill = :skill OR skill IS NULL)
                ORDER BY created_at, id LIMIT :limit
                """, params, (row, index) -> new ReviewListEntry(row.getObject("id", UUID.class),
                row.getObject("knowledge_point_id", UUID.class), row.getObject("lesson_id", UUID.class),
                row.getString("skill") == null ? null : LearningSkill.valueOf(row.getString("skill")),
                ReviewStage.valueOf(row.getString("stage")), row.getTimestamp("created_at").toInstant()));
    }

    @Override
    public void backfillMissingSkill(UUID userId) {
        // Keyed by KP, not lesson: a review can point at a lesson the learner has never opened (moved demo essays).
        jdbc.update("""
                UPDATE review_items r SET skill = c.skill
                FROM knowledge_point_catalog c
                WHERE r.user_id = :userId AND r.skill IS NULL
                  AND c.kp_id = r.knowledge_point_id AND c.skill IS NOT NULL
                """, Map.of("userId", userId));
    }

    @Override
    public void insertPending(UUID userId, List<ReviewCandidate> candidates) {
        if (candidates.isEmpty()) return;
        jdbc.getJdbcOperations().batchUpdate("""
                INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind)
                VALUES (?, ?, ?, ?, 'PENDING', ?, 'ASSESSMENT')
                ON CONFLICT (user_id, knowledge_point_id) WHERE status = 'PENDING' DO NOTHING
                """, candidates, candidates.size(), (statement, review) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, userId);
            statement.setObject(3, review.knowledgePointId());
            statement.setObject(4, review.lessonId());
            statement.setString(5, review.skill() == null ? null : review.skill().name());
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
                UPDATE review_sets SET submitted_at = clock_timestamp(), passed = :passed, request_id = :requestId,
                correct_count = :correct, total_count = :total
                WHERE id = :setId AND submitted_at IS NULL
                """, Map.of("setId", set.id(), "passed", set.passed(), "requestId", set.requestId(),
                "correct", set.correct(), "total", set.total())));
        Map<String, Object> state = new HashMap<>();
        state.put("reviewId", review.id());
        state.put("status", review.status().name());
        state.put("stage", review.stage().name());
        state.put("reason", review.theoryReason() == null ? null : review.theoryReason().name());
        state.put("theoryCount", review.theoryCompletedCount());
        jdbc.update("""
                UPDATE review_items SET status = :status, stage = :stage, theory_reason = :reason,
                theory_completed_count = :theoryCount,
                done_at = CASE WHEN :status = 'PENDING' THEN NULL ELSE clock_timestamp() END
                WHERE id = :reviewId AND status = 'PENDING'
                """, state);
    }

    @Override
    public void deleteOpenSet(UUID userId, UUID reviewId, UUID setId) {
        int deleted = jdbc.update("""
                DELETE FROM review_sets
                WHERE id = :setId AND user_id = :userId AND review_item_id = :reviewId
                  AND submitted_at IS NULL
                """, Map.of("setId", setId, "userId", userId, "reviewId", reviewId));
        if (deleted != 1) throw new IllegalStateException("Review set changed while replacing it");
    }

    @Override
    public List<UUID> assignedPackageIds(UUID userId) {
        return jdbc.queryForList("SELECT DISTINCT package_id FROM review_sets WHERE user_id = :userId",
                Map.of("userId", userId), UUID.class);
    }
}
