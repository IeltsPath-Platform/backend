package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.EssayGradingPort;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CourseGatePolicyIntegrationTest extends ExaminerIntegrationSupport {
    @Test void completedLlmGradeWaitsForExaminerAndRecordsComparison() throws Exception {
        UUID attempt = courseEssay();
        when(grader.available()).thenReturn(true);
        when(grader.grade(any(), anyString())).thenReturn(new EssayGradingPort.EssayGrade(new BigDecimal("6.0"),
                "{\"band\":6.0,\"summary\":\"Clear argument\",\"criteria\":[{\"code\":\"TR\",\"band\":6,\"comment\":\"Develop examples\"}],\"focus\":[\"Support claims\"]}"));
        gradeJobs.gradeBatch();
        assertEquals(0, count("SELECT count(*) FROM assessment_results WHERE attempt_id=?", attempt));
        assertEquals(1, count("SELECT count(*) FROM grading_jobs WHERE id=? AND reason='COURSE_GATE_POLICY'", humanJob(attempt)));
        assertEquals(1, count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND llm_band=6.0 AND examiner_band IS NULL", item(attempt)));
        mvc.perform(get("/api/assessments/attempts/{id}/provisional-feedback",attempt).with(user(learner.toString()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingExaminer").value(true))
                .andExpect(jsonPath("$.essays[0].comments[0]").value("Develop examples"))
                .andExpect(jsonPath("$..band").doesNotExist()).andExpect(jsonPath("$..llmBand").doesNotExist());
        mvc.perform(get("/api/assessments/attempts/{id}/provisional-feedback",attempt).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isNotFound());
        UUID result=openClaimed(attempt);
        save(result,attempt,6.5);
        finalizeResult(result,examiner,200);
        finalizeResult(result,examiner,200);
        assertEquals(1,count("SELECT count(*) FROM assessment_results WHERE attempt_id=? AND status='COMPLETED'",attempt));
        assertEquals(1,eventCount(attempt));
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND llm_band=6.0 AND examiner_band=6.5 AND examiner_id=? AND examiner_graded_at IS NOT NULL AND llm_prompt_version IS NOT NULL",item(attempt),examiner));
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND examiner_criteria->0->>'code'='TR' AND examiner_error_codes=ARRAY['PARAPHRASE_TRAP']::varchar[]",item(attempt)));
        assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM grading_jobs WHERE id=?",String.class,humanJob(attempt)));
        assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM human_reviews WHERE grading_job_id=?",String.class,humanJob(attempt)));
    }

    @Test void lateLlmGradePreservesTheExaminerScoreAndDoesNotEmitAgain() throws Exception {
        UUID attempt=courseEssay();
        UUID result=openClaimed(attempt);
        save(result,attempt,7.0);
        finalizeResult(result,examiner,200);
        when(grader.available()).thenReturn(true);
        when(grader.grade(any(),anyString())).thenReturn(new EssayGradingPort.EssayGrade(new BigDecimal("5.5"),null));
        gradeJobs.gradeBatch();
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND llm_band=5.5 AND examiner_band=7.0",item(attempt)));
        assertEquals(1,eventCount(attempt));
    }

    @Test void failedLlmKeepsCoursePolicyAndExaminerCanFinishWithoutFabricatedComparison() throws Exception {
        UUID attempt=courseEssay();
        when(grader.available()).thenReturn(false);
        gradeJobs.gradeBatch();
        UUID job=humanJob(attempt);
        assertEquals("COURSE_GATE_POLICY",jdbc.queryForObject("SELECT reason FROM grading_jobs WHERE id=?",String.class,job));
        UUID result=openClaimed(attempt);
        save(result,attempt,6.0);
        finalizeResult(result,examiner,200);
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND examiner_band=6.0 AND llm_band IS NULL",item(attempt)));
    }

    @Test void missingEssayBandRollsBackFinalizationAndQueueCompletion() throws Exception {
        UUID attempt=courseEssay();
        UUID result=openClaimed(attempt);
        String body=json.writeValueAsString(Map.of("itemResults",List.of(Map.of("attemptItemId",item(attempt),"score",1,"maxScore",1,"correct",true))));
        mvc.perform(put("/api/assessments/grading/results/{id}/details",result).with(user(examiner.toString()).roles("EXAMINER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        finalizeResult(result,examiner,400);
        assertEquals(0,eventCount(attempt));
        assertEquals("QUEUED",jdbc.queryForObject("SELECT status FROM grading_jobs WHERE id=?",String.class,humanJob(attempt)));
    }

    @Test void unassignedAndUnchosenExaminersCannotGradeOrTakeOver() throws Exception {
        UUID attempt=courseEssay();
        mvc.perform(post("/api/assessments/grading/attempts/{id}/results",attempt).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isForbidden());
        UUID result=openClaimed(attempt);
        save(result,attempt,6.5);
        UUID other=UUID.randomUUID();
        jdbc.update("UPDATE grading_jobs SET claimed_at=CURRENT_TIMESTAMP-INTERVAL '25 hours' WHERE id=?",humanJob(attempt));
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",humanJob(attempt)).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict());
        finalizeResult(result,other,403);
        mvc.perform(get("/api/assessments/grading-jobs/{id}/essay",humanJob(attempt)).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isForbidden());
        finalizeResult(result,examiner,200);
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND examiner_id=?",item(attempt),examiner));
    }

    @Test void omittedEssayDoesNotAutomaticallyCompleteCourseGate() {
        UUID attempt=courseEssay(false);
        assertEquals(0,count("SELECT count(*) FROM assessment_results WHERE attempt_id=?",attempt));
        assertNotNull(humanJob(attempt));
        assertEquals(0,count("SELECT count(*) FROM grading_jobs j JOIN learner_submissions l ON l.id=j.submission_id WHERE l.attempt_item_id=? AND j.grading_mode='AI'",item(attempt)));
    }

    @Test void comparisonUsesTheAssignedSubmissionEvenIfLearnerSendsAnotherEssay() throws Exception {
        UUID attempt=courseEssay();
        UUID original=jdbc.queryForObject("SELECT submission_id FROM grading_jobs WHERE id=?",UUID.class,humanJob(attempt));
        UUID result=openClaimed(attempt);
        submissions.execute(new com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand(learner,item(attempt),"{}",
                com.ieltspath.assessment.domain.vo.Skill.WRITING,"A replacement essay",null,"replacement-"+attempt));
        save(result,attempt,6.5);
        finalizeResult(result,examiner,200);
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons WHERE submission_id=? AND examiner_band=6.5",original));
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=?",item(attempt)));
    }

    @Test void courseHumanJobUsesTheSameFrozenSubmissionAsTheLlmJob() {
        UUID attempt=courseEssay();
        UUID job=humanJob(attempt);
        UUID frozen=jdbc.queryForObject("SELECT submission_id FROM grading_jobs WHERE id=?",UUID.class,job);
        jdbc.update("DELETE FROM human_reviews WHERE grading_job_id=?",job);
        jdbc.update("DELETE FROM grading_jobs WHERE id=?",job);
        submissions.execute(new com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand(learner,item(attempt),"{}",
                com.ieltspath.assessment.domain.vo.Skill.WRITING,"New essay after the frozen selection",null,"late-"+attempt));
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status ->
                essayJobs.enqueueCourseHuman(List.of(item(attempt)),learner,Map.of(item(attempt),frozen)));
        assertEquals(frozen,jdbc.queryForObject("SELECT submission_id FROM grading_jobs WHERE id=?",UUID.class,humanJob(attempt)));
    }

    @Test void comparisonUsesLockedAssignmentIdsInsteadOfANewerUnclaimedJob() throws Exception {
        UUID attempt=courseEssay();
        UUID assigned=jdbc.queryForObject("SELECT submission_id FROM grading_jobs WHERE id=?",UUID.class,humanJob(attempt));
        UUID result=openClaimed(attempt);
        save(result,attempt,6.5);
        UUID replacement=submissions.execute(new com.ieltspath.assessment.application.command.CreateLearnerSubmissionCommand(learner,item(attempt),"{}",
                com.ieltspath.assessment.domain.vo.Skill.WRITING,"A different essay",null,"new-job-"+attempt)).id();
        jdbc.update("INSERT INTO grading_jobs(id,submission_id,user_id,skill,grading_mode,status,idempotency_key) VALUES(?,?,?,'WRITING','HUMAN','QUEUED',?)",UUID.randomUUID(),replacement,learner,"extra-human-"+attempt);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status ->
                comparisons.recordExaminer(attempt,result,examiner,List.of(assigned)));
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons WHERE submission_id=? AND examiner_band=6.5",assigned));
        assertEquals(0,count("SELECT count(*) FROM essay_grade_comparisons WHERE submission_id=?",replacement));
    }

    @Test void regradeReopensTheAssignmentOfTheChosenExaminer() throws Exception {
        UUID attempt=courseEssay();
        UUID first=openClaimed(attempt);
        save(first,attempt,6.5);
        finalizeResult(first,examiner,200);
        UUID next=examiner;
        String response=mvc.perform(post("/api/assessments/grading/attempts/{id}/results",attempt).with(user(next.toString()).roles("EXAMINER")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.resultVersion").value(2)).andReturn().getResponse().getContentAsString();
        UUID second=UUID.fromString(json.readTree(response).get("id").textValue());
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",humanJob(attempt)).with(user(next.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        // Reuse the existing item data only after explicitly saving the regrade details as the new examiner.
        String body=json.writeValueAsString(Map.of("overallBand",7.0,"itemResults",List.of(Map.of("attemptItemId",item(attempt),
                "score",1,"maxScore",1,"correct",true,"feedbackSnapshot","{\"band\":7.0,\"criteria\":[]}"))));
        mvc.perform(put("/api/assessments/grading/results/{id}/details",second).with(user(next.toString()).roles("EXAMINER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        finalizeResult(second,next,200);
        assertEquals(2,eventCount(attempt));
        assertEquals(1,count("SELECT count(*) FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id WHERE l.attempt_item_id=? AND examiner_band=7.0 AND examiner_id=?",item(attempt),next));
    }

    @Test void aRegradeStaysWithTheExaminerTheLearnerChoseAndNoOtherExaminerCanTakeIt() throws Exception {
        UUID attempt=courseEssay();
        UUID first=openClaimed(attempt);
        save(first,attempt,6.5);
        finalizeResult(first,examiner,200);
        mvc.perform(post("/api/assessments/grading/attempts/{id}/results",attempt).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.resultVersion").value(2));
        UUID job=humanJob(attempt);

        // The job is open again, unassigned, and still belongs to the chosen examiner: it is not a shared queue.
        assertEquals(1,count("SELECT count(*) FROM grading_jobs WHERE id=? AND status='QUEUED' AND assigned_examiner_id IS NULL AND requested_examiner_id=?",job,examiner));
        UUID other=UUID.randomUUID();
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",job).with(user(other.toString()).roles("EXAMINER")))
                .andExpect(status().isConflict());
        assertEquals(1,count("SELECT count(*) FROM grading_jobs WHERE id=? AND assigned_examiner_id IS NULL",job));
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",job).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        assertEquals(1,count("SELECT count(*) FROM grading_jobs WHERE id=? AND assigned_examiner_id=?",job,examiner));
    }

    private UUID openClaimed(UUID attempt) throws Exception {
        mvc.perform(post("/api/assessments/grading-jobs/{id}/claim",humanJob(attempt)).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isOk());
        String response=mvc.perform(post("/api/assessments/grading/attempts/{id}/results",attempt).with(user(examiner.toString()).roles("EXAMINER")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").textValue());
    }
    private void save(UUID result,UUID attempt,double band) throws Exception {
        when(errorTypes.activeCodes()).thenReturn(java.util.Set.of("PARAPHRASE_TRAP"));
        String feedback=json.writeValueAsString(Map.of("band",band,"criteria",List.of(Map.of("code","TR","band",band))));
        String body=json.writeValueAsString(Map.of("overallBand",band,"itemResults",List.of(Map.of("attemptItemId",item(attempt),
                "score",1,"maxScore",1,"correct",true,"feedbackSnapshot",feedback)),
                "errors",List.of(Map.of("id",UUID.randomUUID(),"attemptItemId",item(attempt),"errorType","PARAPHRASE_TRAP","explanation","Review wording"))));
        mvc.perform(put("/api/assessments/grading/results/{id}/details",result).with(user(examiner.toString()).roles("EXAMINER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
    }
    private void finalizeResult(UUID result,UUID actor,int expectedStatus) throws Exception {
        mvc.perform(post("/api/assessments/grading/results/{id}/finalize",result).with(user(actor.toString()).roles("EXAMINER")))
                .andExpect(status().is(expectedStatus));
    }
    private int eventCount(UUID attempt) {
        return count("SELECT count(*) FROM outbox_events WHERE event_type='AssessmentCompleted.v2' AND aggregate_id IN (SELECT id::text FROM assessment_results WHERE attempt_id=?)",attempt);
    }
}
