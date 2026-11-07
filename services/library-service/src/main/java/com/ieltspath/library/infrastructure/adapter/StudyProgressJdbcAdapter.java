package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.StudyProgressReader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository @RequiredArgsConstructor
public class StudyProgressJdbcAdapter implements StudyProgressReader {
    private final NamedParameterJdbcTemplate jdbc;
    public Snapshot read(UUID user,List<UUID> topics,Instant now) {
        var p=Map.of("user",user,"topics",topics,"now",Timestamp.from(now));
        var units=jdbc.query("""
            select u.id,t.id topic_id,u.sort_order,u.published_revision,coalesce(v.curriculum_version,0) version
            from vocabulary_topics t left join vocabulary_study_units u on t.id=u.topic_id
              and u.archived_at is null and u.published_revision is not null
            left join vocabulary_study_topic_versions v on v.topic_id=t.id
            where t.id in (:topics) and t.status='ACTIVE'
            order by t.id,u.sort_order,u.id
            """,p,(r,n)->new PublishedUnit(r.getObject("id",UUID.class),r.getObject("topic_id",UUID.class),r.getInt("sort_order"),r.getInt("published_revision"),r.getInt("version")));
        var progress=jdbc.query("select unit_id,first_completed_at,last_completed_at,best_accuracy_percent from vocabulary_study_unit_progress where user_id=:user and topic_id in (:topics)",p,
            (r,n)->new Progress(r.getObject("unit_id",UUID.class),r.getTimestamp("first_completed_at").toInstant(),r.getTimestamp("last_completed_at").toInstant(),r.getInt("best_accuracy_percent")));
        var attempts=jdbc.query("""
            select a.id,a.unit_id,a.mode,count(s.step_id) total,count(s.completed_at) completed
            from vocabulary_study_attempts a left join vocabulary_study_attempt_steps s on s.attempt_id=a.id
            where a.user_id=:user and a.topic_id in (:topics) and a.status='IN_PROGRESS' and a.expires_at>=:now
            group by a.id,a.unit_id,a.mode
            """,p,(r,n)->new ActiveAttempt(r.getObject("id",UUID.class),r.getObject("unit_id",UUID.class),r.getString("mode"),r.getInt("completed"),r.getInt("total")));
        var versions=new HashMap<UUID,Integer>();units.forEach(u->versions.put(u.topicId(),u.curriculumVersion()));
        return new Snapshot(units.stream().filter(u->u.id()!=null).toList(),progress,attempts,versions);
    }
}
