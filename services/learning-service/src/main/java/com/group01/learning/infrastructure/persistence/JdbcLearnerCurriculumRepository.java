package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.aggregate.LearnerCurriculum;
import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.vo.LearningSkill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcLearnerCurriculumRepository implements LearnerCurriculumRepository {
    private final JdbcTemplate jdbc;

    public JdbcLearnerCurriculumRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LearnerCurriculum find(UUID userId) {
        List<TopicProgress> topics = jdbc.query(
                "SELECT topic_id, sequence_order, passed_at, skill, has_topic_test FROM topic_progress WHERE user_id = ?",
                (row, index) -> {
                    Timestamp passed = row.getTimestamp("passed_at");
                    return new TopicProgress(row.getObject("topic_id", UUID.class),
                            row.getObject("sequence_order", Integer.class), passed == null ? null : passed.toInstant(),
                            row.getString("skill") == null ? null : LearningSkill.valueOf(row.getString("skill")),
                            row.getBoolean("has_topic_test"));
                }, userId);
        return LearnerCurriculum.restore(userId, topics);
    }

    /** Writes every topic of the curriculum; a pass already stored keeps its time. */
    @Override
    public void save(LearnerCurriculum curriculum) {
        List<TopicProgress> topics = curriculum.topics();
        if (topics.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO topic_progress (user_id, topic_id, sequence_order, passed_at, skill, has_topic_test)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, topic_id) DO UPDATE SET sequence_order = EXCLUDED.sequence_order,
                passed_at = COALESCE(topic_progress.passed_at, EXCLUDED.passed_at),
                skill = EXCLUDED.skill, has_topic_test = EXCLUDED.has_topic_test, updated_at = clock_timestamp()
                """, topics, topics.size(), (statement, topic) -> {
            statement.setObject(1, curriculum.userId());
            statement.setObject(2, topic.topicId());
            statement.setObject(3, topic.sequenceOrder());
            statement.setTimestamp(4, topic.passedAt() == null ? null : Timestamp.from(topic.passedAt()));
            statement.setString(5, topic.skill() == null ? null : topic.skill().name());
            statement.setBoolean(6, topic.hasTopicTest());
        });
    }
}
