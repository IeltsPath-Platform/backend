package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.LessonPracticePassRepository;
import com.ieltspath.learning.domain.vo.PracticePassReason;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcLessonPracticePassRepository implements LessonPracticePassRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLessonPracticePassRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Map<UUID, PracticePassReason> findByLessons(UUID userId, Collection<UUID> lessonIds) {
        if (lessonIds.isEmpty()) return Map.of();
        Map<UUID, PracticePassReason> found = new HashMap<>();
        jdbc.query("""
                SELECT lesson_id, reason FROM lesson_practice_passes
                WHERE user_id = :userId AND lesson_id IN (:lessonIds)
                """, Map.of("userId", userId, "lessonIds", lessonIds), row -> {
            found.put(row.getObject("lesson_id", UUID.class), PracticePassReason.valueOf(row.getString("reason")));
        });
        return found;
    }

    @Override
    public void insertIfAbsent(UUID userId, Map<UUID, PracticePassReason> reasons) {
        if (reasons.isEmpty()) return;
        jdbc.getJdbcOperations().batchUpdate("""
                INSERT INTO lesson_practice_passes (user_id, lesson_id, reason) VALUES (?, ?, ?)
                ON CONFLICT (user_id, lesson_id) DO NOTHING
                """, reasons.entrySet(), reasons.size(), (statement, entry) -> {
            statement.setObject(1, userId);
            statement.setObject(2, entry.getKey());
            statement.setString(3, entry.getValue().name());
        });
    }
}
