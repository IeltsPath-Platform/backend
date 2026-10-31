package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLearnerCourseUnlockRepository implements LearnerCourseUnlockRepository {
    private final JdbcTemplate jdbc;

    public JdbcLearnerCourseUnlockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LearnerCourseUnlock> findByRequestId(UUID requestId) {
        return jdbc.query("SELECT * FROM learner_course_unlocks WHERE request_id = ?",
                (row, index) -> map(row), requestId).stream().findFirst();
    }

    @Override
    public List<LearnerCourseUnlock> findByUser(UUID userId) {
        return jdbc.query("SELECT * FROM learner_course_unlocks WHERE user_id = ? ORDER BY created_at, id",
                (row, index) -> map(row), userId);
    }

    @Override
    public List<LearnerCourseUnlock> findPending(UUID userId) {
        return jdbc.query("SELECT * FROM learner_course_unlocks WHERE user_id = ? AND status = 'PENDING' "
                + "ORDER BY created_at, id", (row, index) -> map(row), userId);
    }

    @Override
    public List<LearnerCourseUnlock> findActive(UUID userId, UUID courseId) {
        return jdbc.query("SELECT * FROM learner_course_unlocks WHERE user_id = ? AND course_id = ? "
                + "AND status = 'ACTIVE' ORDER BY created_at, id", (row, index) -> map(row), userId, courseId);
    }

    @Override
    public LearnerCourseUnlock createPending(UUID userId, UUID courseId, LearningSkill skill, int pointCost,
                                             UUID requestId) {
        try {
            jdbc.queryForObject("INSERT INTO learner_course_unlocks "
                            + "(user_id, course_id, skill, point_cost, request_id, status) "
                            + "VALUES (?, ?, ?, ?, ?, 'PENDING') RETURNING id",
                    UUID.class, userId, courseId, skill == null ? null : skill.name(), pointCost, requestId);
            return findByRequestId(requestId).orElseThrow();
        } catch (DuplicateKeyException exception) {
            throw exception;
        }
    }

    @Override
    public void activate(UUID requestId, UUID ledgerEntryId) {
        int updated = jdbc.update("UPDATE learner_course_unlocks SET status = 'ACTIVE', ledger_entry_id = ?, "
                + "activated_at = clock_timestamp() WHERE request_id = ? AND status = 'PENDING'",
                ledgerEntryId, requestId);
        if (updated == 0 && findByRequestId(requestId).filter(row -> row.status() == LearnerCourseUnlock.Status.ACTIVE
                && java.util.Objects.equals(ledgerEntryId, row.ledgerEntryId())).isEmpty()) {
            throw new IllegalStateException("Pending course unlock was not found");
        }
    }

    @Override
    public void cancel(UUID requestId) {
        jdbc.update("UPDATE learner_course_unlocks SET status = 'CANCELLED' "
                + "WHERE request_id = ? AND status = 'PENDING'", requestId);
    }

    private static LearnerCourseUnlock map(ResultSet row) throws SQLException {
        String skill = row.getString("skill");
        return new LearnerCourseUnlock(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getObject("course_id", UUID.class), skill == null ? null : LearningSkill.valueOf(skill),
                row.getInt("point_cost"), row.getObject("request_id", UUID.class),
                LearnerCourseUnlock.Status.valueOf(row.getString("status")),
                row.getObject("ledger_entry_id", UUID.class));
    }
}
