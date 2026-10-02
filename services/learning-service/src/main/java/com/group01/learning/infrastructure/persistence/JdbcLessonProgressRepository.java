package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.aggregate.LessonProgress;
import com.group01.learning.domain.repository.LessonProgressRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Repository
public class JdbcLessonProgressRepository implements LessonProgressRepository {
    private final JdbcTemplate jdbc;

    public JdbcLessonProgressRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LessonProgress> find(UUID userId, UUID lessonId) {
        return jdbc.query("SELECT * FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
                (row, index) -> lesson(row), userId, lessonId).stream().findFirst();
    }

    @Override
    public Map<UUID, LessonProgress> findByTopic(UUID userId, UUID topicId) {
        Map<UUID, LessonProgress> lessons = new HashMap<>();
        jdbc.query("SELECT * FROM lesson_progress WHERE user_id = ? AND topic_id = ?", (RowCallbackHandler) row -> {
            LessonProgress lesson = lesson(row);
            lessons.put(lesson.lessonId(), lesson);
        }, userId, topicId);
        return lessons;
    }

    @Override
    public List<LessonProgress> findCompleted(UUID userId) {
        return jdbc.query("SELECT * FROM lesson_progress WHERE user_id = ? AND completed_at IS NOT NULL",
                (row, index) -> lesson(row), userId);
    }

    @Override
    public Map<UUID, Integer> completedCountsByTopic(UUID userId) {
        Map<UUID, Integer> counts = new HashMap<>();
        jdbc.query("SELECT topic_id, count(*) AS total FROM lesson_progress "
                        + "WHERE user_id = ? AND completed_at IS NOT NULL GROUP BY topic_id",
                (RowCallbackHandler) row -> counts.put(row.getObject("topic_id", UUID.class), row.getInt("total")),
                userId);
        return counts;
    }

    /** The aggregate holds the whole row; an earlier completion time is kept. */
    @Override
    public void save(LessonProgress progress) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids,
                    passed_block_ids, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id, lesson_id) DO UPDATE
                    SET topic_id = EXCLUDED.topic_id, lesson_sort_order = EXCLUDED.lesson_sort_order,
                    knowledge_point_ids = EXCLUDED.knowledge_point_ids, passed_block_ids = EXCLUDED.passed_block_ids,
                    completed_at = COALESCE(lesson_progress.completed_at, EXCLUDED.completed_at),
                    updated_at = clock_timestamp()
                    """);
            statement.setObject(1, progress.userId());
            statement.setObject(2, progress.lessonId());
            statement.setObject(3, progress.topicId());
            statement.setInt(4, progress.sortOrder());
            statement.setArray(5, connection.createArrayOf("uuid", progress.knowledgePointIds().toArray()));
            statement.setArray(6, connection.createArrayOf("text",
                    progress.passedBlockIds().stream().map(UUID::toString).toArray()));
            statement.setTimestamp(7, progress.completedAt() == null ? null : Timestamp.from(progress.completedAt()));
            return statement;
        });
    }

    private static LessonProgress lesson(ResultSet row) throws SQLException {
        Timestamp completed = row.getTimestamp("completed_at");
        return LessonProgress.restore(row.getObject("user_id", UUID.class), row.getObject("lesson_id", UUID.class),
                row.getObject("topic_id", UUID.class), row.getInt("lesson_sort_order"),
                SqlArrays.uuids(row.getArray("knowledge_point_ids")),
                new LinkedHashSet<>(SqlArrays.uuids(row.getArray("passed_block_ids"))),
                completed == null ? null : completed.toInstant());
    }
}
