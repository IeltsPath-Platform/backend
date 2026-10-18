package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.EssayGradeComparisonStore;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@Repository
public class JdbcEssayGradeComparisonStore implements EssayGradeComparisonStore {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcEssayGradeComparisonStore(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void recordExaminer(UUID attempt,UUID result,UUID examiner,Collection<UUID> assignedSubmissions) {
        String source = assignedSubmissions.isEmpty()
                ? " AND EXISTS(SELECT 1 FROM grading_jobs j WHERE j.submission_id=l.id AND j.idempotency_key='gate-essay:'||i.id||':ai')"
                : " AND l.id IN (:assignedSubmissions)";
        jdbc.update("""
                INSERT INTO essay_grade_comparisons(submission_id,attempt_type,task,examiner_band,
                  examiner_criteria,examiner_error_codes,examiner_id,examiner_graded_at)
                SELECT l.id,a.attempt_type,i.answer_snapshot->'answerSpec'->>'task',
                  (ir.feedback_snapshot->>'band')::numeric,COALESCE(ir.feedback_snapshot->'criteria','[]'::jsonb),
                  ARRAY(SELECT DISTINCT e.error_type FROM error_analysis_items e WHERE e.item_result_id=ir.id ORDER BY e.error_type),
                  :examiner,CURRENT_TIMESTAMP
                FROM learner_submissions l JOIN attempt_items i ON i.id=l.attempt_item_id
                JOIN attempt_sections s ON s.id=i.attempt_section_id JOIN assessment_attempts a ON a.id=s.attempt_id
                JOIN item_results ir ON ir.attempt_item_id=i.id AND ir.result_id=:result
                WHERE a.id=:attempt AND l.skill='WRITING' AND l.status='SUBMITTED'
                  AND i.answer_snapshot->'answerSpec'->>'type'='ESSAY'
                  AND jsonb_typeof(ir.feedback_snapshot->'band')='number'
                """ + source + "\n" + """
                ON CONFLICT(submission_id) DO UPDATE SET examiner_band=EXCLUDED.examiner_band,
                  examiner_criteria=EXCLUDED.examiner_criteria,examiner_error_codes=EXCLUDED.examiner_error_codes,
                  examiner_id=EXCLUDED.examiner_id,examiner_graded_at=EXCLUDED.examiner_graded_at
                """,new MapSqlParameterSource().addValue("attempt",attempt).addValue("result",result)
                .addValue("examiner",examiner).addValue("assignedSubmissions",assignedSubmissions));
    }
}
