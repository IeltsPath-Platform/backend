package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.aggregate.PracticeTestAssignment;
import com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcPracticeTestAssignmentRepository implements PracticeTestAssignmentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcPracticeTestAssignmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final RowMapper<PracticeTestAssignment> ROW = (r,i) -> PracticeTestAssignment.restore(
            r.getObject("id",UUID.class),r.getObject("user_id",UUID.class),r.getObject("course_id",UUID.class),
            r.getObject("package_id",UUID.class),r.getObject("package_version_id",UUID.class),
            r.getTimestamp("assigned_at").toInstant(),r.getObject("consumed_attempt_id",UUID.class),
            r.getTimestamp("consumed_at")==null ? null : r.getTimestamp("consumed_at").toInstant(),r.getBigDecimal("percent"));
    @Override public List<PracticeTestAssignment> findByCourse(UUID userId,UUID courseId) {
        return jdbc.query("SELECT * FROM practice_test_assignments WHERE user_id=:user AND course_id=:course ORDER BY assigned_at,id",
                Map.of("user",userId,"course",courseId),ROW);
    }
    @Override public Optional<PracticeTestAssignment> find(UUID userId,UUID packageId) {
        return jdbc.query("SELECT * FROM practice_test_assignments WHERE user_id=:user AND package_id=:package",
                Map.of("user",userId,"package",packageId),ROW).stream().findFirst();
    }
    @Override public Optional<PracticeTestAssignment> findOpenForAttempt(UUID userId,UUID versionId,Instant completedAt) {
        return jdbc.query("""
                SELECT * FROM practice_test_assignments WHERE user_id=:user AND package_version_id=:version
                AND consumed_attempt_id IS NULL AND assigned_at<=:completed ORDER BY assigned_at,id LIMIT 1 FOR UPDATE
                """,Map.of("user",userId,"version",versionId,"completed",Timestamp.from(completedAt)),ROW).stream().findFirst();
    }
    @Override public void save(PracticeTestAssignment a) {
        jdbc.update("""
                INSERT INTO practice_test_assignments(id,user_id,course_id,package_id,package_version_id,assigned_at,
                    consumed_attempt_id,consumed_at,percent)
                VALUES (:id,:user,:course,:package,:version,:assigned,:attempt,:consumed,:percent)
                ON CONFLICT (user_id,package_id) DO UPDATE SET consumed_attempt_id=EXCLUDED.consumed_attempt_id,
                    consumed_at=EXCLUDED.consumed_at,percent=EXCLUDED.percent
                WHERE practice_test_assignments.id=EXCLUDED.id AND practice_test_assignments.consumed_attempt_id IS NULL
                """,new MapSqlParameterSource("id",a.id()).addValue("user",a.userId()).addValue("course",a.courseId())
                .addValue("package",a.packageId()).addValue("version",a.packageVersionId())
                .addValue("assigned",Timestamp.from(a.assignedAt())).addValue("attempt",a.consumedAttemptId())
                .addValue("consumed",a.consumedAt()==null?null:Timestamp.from(a.consumedAt())).addValue("percent",a.percent()));
    }
}
