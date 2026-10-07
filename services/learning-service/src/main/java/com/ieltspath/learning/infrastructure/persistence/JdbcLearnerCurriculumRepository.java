package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcLearnerCurriculumRepository implements LearnerCurriculumRepository {
    private final JdbcTemplate jdbc;

    public JdbcLearnerCurriculumRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LearnerCurriculum find(UUID userId) {
        List<TopicProgress> topics = jdbc.query(
                "SELECT topic_id, course_id, sequence_order, passed_at, skills, has_topic_test "
                        + "FROM topic_progress WHERE user_id = ?",
                (row, index) -> {
                    Timestamp passed = row.getTimestamp("passed_at");
                    return TopicProgress.withSkills(row.getObject("topic_id", UUID.class),
                            row.getObject("course_id", UUID.class),
                            row.getObject("sequence_order", Integer.class), passed == null ? null : passed.toInstant(),
                            skills(row.getArray("skills")), row.getBoolean("has_topic_test"));
                }, userId);
        return LearnerCurriculum.restore(userId, topics);
    }

    /** Writes every topic of the curriculum; a pass already stored keeps its time. */
    @Override
    public void save(LearnerCurriculum curriculum) {
        List<TopicProgress> topics = curriculum.topics();
        if (topics.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO topic_progress (user_id, topic_id, course_id, sequence_order, passed_at, skill, skills,
                has_topic_test)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, topic_id) DO UPDATE SET sequence_order = EXCLUDED.sequence_order,
                passed_at = COALESCE(topic_progress.passed_at, EXCLUDED.passed_at),
                course_id = EXCLUDED.course_id, skill = EXCLUDED.skill, skills = EXCLUDED.skills,
                has_topic_test = EXCLUDED.has_topic_test, updated_at = clock_timestamp()
                """, topics, topics.size(), (statement, topic) -> {
            statement.setObject(1, curriculum.userId());
            statement.setObject(2, topic.topicId());
            statement.setObject(3, topic.courseId());
            statement.setObject(4, topic.sequenceOrder());
            statement.setTimestamp(5, topic.passedAt() == null ? null : Timestamp.from(topic.passedAt()));
            statement.setString(6, topic.skill() == null ? null : topic.skill().name());
            statement.setArray(7, statement.getConnection().createArrayOf("varchar",
                    topic.skills().stream().map(Enum::name).toArray()));
            statement.setBoolean(8, topic.hasTopicTest());
        });
    }

    private static Set<LearningSkill> skills(Array array) throws SQLException {
        if (array == null) return Set.of();
        try {
            return Arrays.stream((Object[]) array.getArray()).map(value -> LearningSkill.valueOf((String) value))
                    .collect(Collectors.toUnmodifiableSet());
        } finally {
            array.free();
        }
    }
}
