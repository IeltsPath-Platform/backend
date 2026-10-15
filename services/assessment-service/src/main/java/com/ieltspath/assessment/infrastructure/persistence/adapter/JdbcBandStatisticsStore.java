package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.domain.vo.BandMeasurement;
import com.ieltspath.assessment.domain.vo.Skill;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcBandStatisticsStore implements BandStatisticsStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final EntityManager entities;
    private final ObjectMapper json;
    public JdbcBandStatisticsStore(NamedParameterJdbcTemplate jdbc,EntityManager entities,ObjectMapper json) {
        this.jdbc=jdbc;this.entities=entities;this.json=json;
    }
    public void freeze(UUID result,List<BandMeasurement> snapshot) {
        entities.flush();
        try {
            jdbc.update("UPDATE assessment_results SET band_snapshot=CAST(:snapshot AS jsonb) WHERE id=:id AND band_snapshot IS NULL",
                    Map.of("id",result,"snapshot",json.writeValueAsString(snapshot)));
        } catch(JsonProcessingException e) { throw new IllegalStateException("Cannot serialize band measurements",e); }
    }
    public void markGradedSkills(UUID result,Collection<Skill> skills,boolean verified) {
        if(skills.isEmpty()) return;
        entities.flush();
        jdbc.update("UPDATE skill_scores SET grader_verified=:verified WHERE result_id=:result AND skill IN (:skills)",
                Map.of("result",result,"verified",verified,"skills",skills.stream().map(Enum::name).toList()));
    }
    public Set<Skill> verifiedSkills(UUID result) {
        entities.flush();
        return Set.copyOf(jdbc.query("SELECT skill FROM skill_scores WHERE result_id=:id AND grader_verified",
                Map.of("id",result),(rs,n)->Skill.valueOf(rs.getString("skill"))));
    }
    // Select the completed version before checking measurement eligibility. A newer ineligible result replaces,
    // rather than resurrects, an older measured result; a pending version leaves the completed result visible.
    private static final String SERIES="""
        WITH newest AS (
          SELECT DISTINCT ON (a.id) a.id AS attempt_id,a.submitted_at,r.id AS result_id,r.result_version,r.band_snapshot,
            EXISTS(SELECT 1 FROM assessment_results pending WHERE pending.attempt_id=a.id
                   AND pending.result_version>r.result_version AND pending.status IN ('DRAFT','PROCESSING')) AS pending_regrade
          FROM assessment_attempts a JOIN assessment_results r ON r.attempt_id=a.id AND r.status='COMPLETED'
          WHERE a.user_id=:user AND a.attempt_type=:kind AND a.submitted_at>=:from AND a.submitted_at<:until
          ORDER BY a.id,r.result_version DESC
        ), eligible AS (
          SELECT n.*,(m->>'band')::double precision AS band,m->>'gradingSource' AS source,m->>'scoringVersion' AS scoring
          FROM newest n CROSS JOIN LATERAL jsonb_array_elements(COALESCE(n.band_snapshot,'[]'::jsonb)) m
          WHERE m->>'skill'=:skill AND m->>'measurementQuality'='MEASURED'
            AND m->>'eligibilityReason' IS NULL AND (m->>'band')::double precision BETWEEN 0 AND 9
        )
        """;
    public Page history(UUID user,Instant from,Instant until,String kind,String skill,Cursor cursor,int limit) {
        var p=new MapSqlParameterSource().addValue("user",user).addValue("from",Timestamp.from(from))
                .addValue("until",Timestamp.from(until)).addValue("kind",kind).addValue("skill",skill).addValue("limit",limit);
        var summary=jdbc.query(SERIES+"""
            SELECT (SELECT band FROM eligible ORDER BY submitted_at,attempt_id LIMIT 1) AS baseline,
                   (SELECT band FROM eligible ORDER BY submitted_at DESC,attempt_id DESC LIMIT 1) AS latest,
                   (SELECT min(submitted_at) FROM eligible) AS data_from
            """,p,(rs,n)->new Page(List.of(),(Double)rs.getObject("baseline"),(Double)rs.getObject("latest"),
                    rs.getTimestamp("data_from")==null?null:rs.getTimestamp("data_from").toInstant())).getFirst();
        String after="";
        if(cursor!=null) {
            after=" WHERE (submitted_at,attempt_id) < (:cursorTime,:cursorId)";
            p.addValue("cursorTime",Timestamp.from(cursor.submittedAt())).addValue("cursorId",cursor.attemptId());
        }
        var points=jdbc.query(SERIES+"SELECT * FROM eligible"+after+" ORDER BY submitted_at DESC,attempt_id DESC LIMIT :limit",p,
                (rs,n)->new Point(rs.getObject("attempt_id",UUID.class),rs.getInt("result_version"),
                        rs.getTimestamp("submitted_at").toInstant(),rs.getDouble("band"),rs.getBoolean("pending_regrade"),
                        rs.getString("source"),"MEASURED",rs.getString("scoring")));
        return new Page(points,summary.baseline(),summary.latest(),summary.dataFrom());
    }
}
