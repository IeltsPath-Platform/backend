package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.PracticeProgressRepository;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcPracticeProgressRepository implements PracticeProgressRepository {
    private final JdbcTemplate jdbc;

    public JdbcPracticeProgressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<GradedSpeaking> gradedSpeaking(UUID userId, SpeakingScope scope) {
        // A part test holds the recordings of one part; a full test has no single part. A lesson's practice answer has a
        // lesson id and is not an answer to a catalog test.
        return jdbc.query("""
                SELECT * FROM (
                    SELECT DISTINCT ON (s.package_version_id, part.part_no)
                           s.id, s.package_version_id, part.part_no, COALESCE(s.graded_at, s.updated_at) AS done_at
                    FROM speaking_submissions s
                    LEFT JOIN LATERAL (SELECT CASE WHEN s.scope = 'PART' THEN MIN(r.part_no) END AS part_no
                                       FROM speaking_recordings r WHERE r.submission_id = s.id) part ON TRUE
                    WHERE s.user_id = ? AND s.status = 'GRADED' AND s.scope = ? AND s.lesson_id IS NULL
                    ORDER BY s.package_version_id, part.part_no, COALESCE(s.graded_at, s.updated_at) DESC, s.id
                ) latest
                ORDER BY done_at DESC
                LIMIT ?
                """, (row, index) -> new GradedSpeaking(row.getObject("id", UUID.class),
                        row.getObject("package_version_id", UUID.class), (Integer) row.getObject("part_no"),
                        row.getTimestamp("done_at").toInstant()),
                userId, scope.name(), LIMIT);
    }

    @Override
    public List<GradedWriting> gradedFreeWriting(UUID userId) {
        return jdbc.query("""
                SELECT * FROM (
                    SELECT DISTINCT ON (question_version_id) id, question_version_id, submitted_at AS done_at
                    FROM lesson_writing_submissions
                    WHERE user_id = ? AND source = 'FREE_WRITING' AND status = 'GRADED'
                    ORDER BY question_version_id, submitted_at DESC, id
                ) latest
                ORDER BY done_at DESC
                LIMIT ?
                """, (row, index) -> new GradedWriting(row.getObject("id", UUID.class),
                        row.getObject("question_version_id", UUID.class), row.getTimestamp("done_at").toInstant()),
                userId, LIMIT);
    }
}
