package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.CourseScoreConfigRepository;
import com.ieltspath.learning.domain.vo.CourseScoreConfig;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCourseScoreConfigRepository implements CourseScoreConfigRepository {
    private static final String COLUMNS = """
            course_id, process_weight, mid_weight, final_weight, practice_share, practice_test_share, pass_threshold,
            mid_remediation_below""";
    private static final RowMapper<CourseScoreConfig> CONFIG = (row, index) -> new CourseScoreConfig(
            row.getObject("course_id", UUID.class), row.getBigDecimal("process_weight"),
            row.getBigDecimal("mid_weight"), row.getBigDecimal("final_weight"), row.getBigDecimal("practice_share"),
            row.getBigDecimal("practice_test_share"), row.getBigDecimal("pass_threshold"),
            row.getBigDecimal("mid_remediation_below"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCourseScoreConfigRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CourseScoreConfig> find(UUID courseId) {
        String where = courseId == null ? "course_id IS NULL" : "course_id = :courseId";
        return jdbc.query("SELECT " + COLUMNS + " FROM course_score_configs WHERE " + where,
                new MapSqlParameterSource("courseId", courseId), CONFIG).stream().findFirst();
    }

    @Override
    public CourseScoreConfig effective(UUID courseId) {
        Optional<CourseScoreConfig> own = courseId == null ? Optional.empty() : find(courseId);
        return own.or(() -> find(null)).orElseThrow(() -> new IllegalStateException("Default score config is missing"));
    }

    @Override
    public void save(CourseScoreConfig config, UUID updatedBy) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", UUID.randomUUID())
                .addValue("courseId", config.courseId()).addValue("process", config.processWeight())
                .addValue("mid", config.midWeight()).addValue("final", config.finalWeight())
                .addValue("practice", config.practiceShare()).addValue("practiceTest", config.practiceTestShare())
                .addValue("pass", config.passThreshold()).addValue("below", config.midRemediationBelow())
                .addValue("updatedBy", updatedBy);
        if (config.courseId() == null) {
            jdbc.update("""
                    UPDATE course_score_configs SET process_weight = :process, mid_weight = :mid,
                    final_weight = :final, practice_share = :practice, practice_test_share = :practiceTest,
                    pass_threshold = :pass, mid_remediation_below = :below, updated_by = :updatedBy,
                    updated_at = clock_timestamp() WHERE course_id IS NULL
                    """, params);
            return;
        }
        jdbc.update("""
                INSERT INTO course_score_configs (id, course_id, process_weight, mid_weight, final_weight,
                    practice_share, practice_test_share, pass_threshold, mid_remediation_below, updated_by)
                VALUES (:id, :courseId, :process, :mid, :final, :practice, :practiceTest, :pass, :below, :updatedBy)
                ON CONFLICT (course_id) DO UPDATE SET process_weight = EXCLUDED.process_weight,
                    mid_weight = EXCLUDED.mid_weight, final_weight = EXCLUDED.final_weight,
                    practice_share = EXCLUDED.practice_share, practice_test_share = EXCLUDED.practice_test_share,
                    pass_threshold = EXCLUDED.pass_threshold, mid_remediation_below = EXCLUDED.mid_remediation_below,
                    updated_by = EXCLUDED.updated_by, updated_at = clock_timestamp()
                """, params);
    }
}
