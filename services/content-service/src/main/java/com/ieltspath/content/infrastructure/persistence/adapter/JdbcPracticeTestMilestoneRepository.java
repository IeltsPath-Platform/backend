package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.repository.PracticeTestMilestoneRepository;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class JdbcPracticeTestMilestoneRepository implements PracticeTestMilestoneRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcPracticeTestMilestoneRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<PracticeTestMilestone> findByCourse(UUID courseId, boolean publishedOnly) {
        return jdbc.query("""
                SELECT p.id, v.id version_id, p.code, p.title, m.after_topic_sequence
                FROM course_practice_test_milestones m JOIN content_packages p ON p.id=m.package_id
                LEFT JOIN content_package_versions v ON v.id=p.current_published_version_id
                    AND v.package_id=p.id AND v.status='PUBLISHED'
                WHERE m.course_id=:courseId
                  AND (NOT :publishedOnly OR (p.package_type='PRACTICE_TEST' AND p.status='PUBLISHED' AND v.id IS NOT NULL))
                ORDER BY m.after_topic_sequence, p.code, p.id LIMIT 100
                """, Map.of("courseId", courseId, "publishedOnly", publishedOnly), (r, i) ->
                new PracticeTestMilestone(r.getObject("id", UUID.class), r.getObject("version_id", UUID.class),
                        r.getString("code"), r.getString("title"), r.getInt("after_topic_sequence")));
    }

    @Override public void lockCourse(UUID courseId) {
        jdbc.getJdbcTemplate().query("SELECT pg_advisory_xact_lock(782184338)", r -> { });
        jdbc.queryForList("SELECT id FROM courses WHERE id=:id FOR UPDATE", Map.of("id", courseId), UUID.class);
    }

    @Override public Set<UUID> packagesInOtherCourses(UUID courseId, Collection<UUID> packageIds) {
        if (packageIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("SELECT package_id FROM course_practice_test_milestones "
                + "WHERE course_id<>:course AND package_id IN (:ids)", Map.of("course",courseId,"ids",packageIds),UUID.class));
    }

    @Override public int topicCount(UUID courseId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM topics t WHERE t.course_id=:id AND t.status='ACTIVE'
                AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id=t.id AND l.status='PUBLISHED')
                """, Map.of("id", courseId), Integer.class);
    }

    @Override public Set<UUID> publishedPracticeTests(Collection<UUID> packageIds) {
        if (packageIds.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.queryForList("""
                SELECT p.id FROM content_packages p JOIN content_package_versions v
                ON v.id=p.current_published_version_id AND v.package_id=p.id
                WHERE p.id IN (:ids) AND p.package_type='PRACTICE_TEST'
                  AND p.status='PUBLISHED' AND v.status='PUBLISHED'
                FOR SHARE OF p, v
                """, Map.of("ids", packageIds), UUID.class));
    }

    @Override public void replace(UUID courseId, List<PracticeTestMilestone> milestones) {
        jdbc.update("DELETE FROM course_practice_test_milestones WHERE course_id=:id", Map.of("id", courseId));
        var batch = milestones.stream().map(m -> new MapSqlParameterSource("courseId", courseId)
                .addValue("packageId", m.packageId()).addValue("sequence", m.afterTopicSequence()))
                .toArray(MapSqlParameterSource[]::new);
        if (batch.length > 0) jdbc.batchUpdate("""
                INSERT INTO course_practice_test_milestones(course_id,package_id,after_topic_sequence)
                VALUES (:courseId,:packageId,:sequence)
                """, batch);
    }
}
