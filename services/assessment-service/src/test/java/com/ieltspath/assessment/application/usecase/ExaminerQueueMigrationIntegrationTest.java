package com.ieltspath.assessment.application.usecase;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class ExaminerQueueMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    @Test void upgradesDuplicateLegacyJobsAndFindsOmittedCourseEssays() {
        var source=new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        Flyway.configure().dataSource(source).target("8").load().migrate();
        var jdbc=new JdbcTemplate(source);
        UUID user=UUID.randomUUID(),attempt=UUID.randomUUID(),section=UUID.randomUUID(),item=UUID.randomUUID(),omitted=UUID.randomUUID(),submission=UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_attempts(id,user_id,package_version_id,attempt_type,mode,channel,status) VALUES(?,?,?,'COURSE_GATE','STANDARD','WEB','SUBMITTED')",attempt,user,UUID.randomUUID());
        jdbc.update("INSERT INTO attempt_sections(id,attempt_id,content_section_id,section_snapshot,sort_order) VALUES(?,?,?,'{\"skill\":\"WRITING\"}',1)",section,attempt,UUID.randomUUID());
        for (UUID id:java.util.List.of(item,omitted))
            jdbc.update("INSERT INTO attempt_items(id,attempt_section_id,question_version_id,sort_order,question_snapshot,answer_snapshot) VALUES(?,?,?,?,'{\"stem\":\"Discuss\"}','{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_2\"}}')",id,section,UUID.randomUUID(),id.equals(item)?1:2);
        jdbc.update("INSERT INTO learner_submissions(id,user_id,attempt_item_id,prompt_snapshot,skill,text_payload,status,submission_key) VALUES(?,?,?,'{}','WRITING','Essay','SUBMITTED','legacy-essay')",submission,user,item);
        for (String key:java.util.List.of("legacy-ai-one","legacy-ai-two"))
            jdbc.update("INSERT INTO grading_jobs(id,submission_id,user_id,skill,grading_mode,status,point_cost_snapshot,idempotency_key,llm_band) VALUES(?,?,?,'WRITING','AI','COMPLETED',0,?,6.0)",UUID.randomUUID(),submission,user,key);
        Flyway.configure().dataSource(source).load().migrate();
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM grading_jobs WHERE grading_mode='HUMAN' AND reason='COURSE_GATE_POLICY'",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM human_reviews",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM learner_submissions WHERE text_payload='' AND prompt_snapshot->>'omitted'='true'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM essay_grade_comparisons WHERE submission_id=? AND llm_band=6.0",Integer.class,submission));
    }
}
