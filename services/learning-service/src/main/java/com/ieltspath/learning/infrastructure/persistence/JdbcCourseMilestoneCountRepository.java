package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.CourseMilestoneCountRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCourseMilestoneCountRepository implements CourseMilestoneCountRepository {
    private final JdbcTemplate jdbc;

    public JdbcCourseMilestoneCountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Its own transaction, so a read-only request such as the score view can still keep the count. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfChanged(UUID courseId, int count) {
        jdbc.update("""
                INSERT INTO course_milestone_counts (course_id, milestone_count) VALUES (?, ?)
                ON CONFLICT (course_id) DO UPDATE SET milestone_count = EXCLUDED.milestone_count,
                refreshed_at = clock_timestamp()
                WHERE course_milestone_counts.milestone_count IS DISTINCT FROM EXCLUDED.milestone_count
                """, courseId, count);
    }

    @Override
    public Optional<Integer> find(UUID courseId) {
        return jdbc.query("SELECT milestone_count FROM course_milestone_counts WHERE course_id = ?",
                (rows, index) -> rows.getInt(1), courseId).stream().findFirst();
    }
}
