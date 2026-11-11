package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.exception.EssayGradingException;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.domain.service.ExaminerErrorRecall;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEvaluationEssayFailureTest {
    private static final String ESSAY_ONE = "essay one";
    private static final String ESSAY_TWO = "essay two";
    private static final String CRITERIA = "[{\"code\":\"TR\",\"band\":6.0,\"comment\":\"ok\"}]";

    @Test
    void anEssayTheModelCannotGradeIsCountedWithoutFailingTheRun() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(PreparedStatementCreator.class))).thenReturn(1);
        EssayGradingPort grader = mock(EssayGradingPort.class);
        when(grader.grade(any(), eq(ESSAY_ONE))).thenReturn(new EssayGradingPort.EssayGrade(
                new BigDecimal("6.0"), "{\"criteria\":[{\"code\":\"TR\",\"band\":6.0}],\"corrections\":[]}", "rubric@1+tplv1"));
        when(grader.grade(any(), eq(ESSAY_TWO))).thenThrow(new EssayGradingException("INVALID_GRADE"));
        AiEvaluationService service = service(jdbc, grader);

        service.evaluate(UUID.randomUUID(), "TASK_2", List.of(gold(ESSAY_ONE, List.of()), gold(ESSAY_TWO, List.of())),
                "rubric@1+tplv1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(sql.capture(), args.capture());
        assertTrue(sql.getValue().contains("status='DONE'"));
        Object[] persisted = args.getValue();
        // Half the essays were invalid: above the 0.05 limit, so the run does not pass.
        assertEquals(0, new BigDecimal("0.50000").compareTo((BigDecimal) persisted[11]));
        assertEquals(false, persisted[5]);
    }

    @Test
    void examinerErrorExcerptMustBeAnExactPartOfTheEssay() {
        AiEvaluationService service = service(mock(JdbcTemplate.class), mock(EssayGradingPort.class));

        assertThrows(IllegalArgumentException.class, () -> service.addManual("TASK_2", "prompt", null, ESSAY_ONE,
                new BigDecimal("6.0"), List.of(), List.of(), List.of(
                        new AiEvaluationService.ExaminerError("not in the essay", "GRAMMAR", "LANGUAGE")),
                UUID.randomUUID()));
    }

    @Test
    void dataErrorsAreRejectedForTaskTwo() {
        AiEvaluationService service = service(mock(JdbcTemplate.class), mock(EssayGradingPort.class));

        assertThrows(IllegalArgumentException.class, () -> service.addManual("TASK_2", "prompt", null, ESSAY_ONE,
                new BigDecimal("6.0"), List.of(), List.of(), List.of(
                        new AiEvaluationService.ExaminerError("essay", "CHART_DATA", "DATA")),
                UUID.randomUUID()));
    }

    @Test
    void p95IsTheNearestRankValue() {
        List<Integer> latencies = java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList();

        assertEquals(19, AiEvaluationService.percentile95(latencies));
        assertEquals(null, AiEvaluationService.percentile95(List.of()));
    }

    @Test
    void aReplyPastTheGradingTimeoutIsCountedAsThatEssaysFailure() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(PreparedStatementCreator.class))).thenReturn(1);
        EssayGradingPort grader = mock(EssayGradingPort.class);
        when(grader.grade(any(), eq(ESSAY_ONE))).thenAnswer(invocation -> {
            Thread.sleep(3_000);
            return new EssayGradingPort.EssayGrade(new BigDecimal("6.0"), null, "rubric@1+tplv1");
        });
        AiEvaluationService service = service(jdbc, grader, 1);

        service.evaluate(UUID.randomUUID(), "TASK_2", List.of(gold(ESSAY_ONE, List.of())), "rubric@1+tplv1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(sql.capture(), args.capture());
        assertTrue(sql.getValue().contains("status='DONE'"));
        // The only essay was invalid: the rate is 1, so the run does not pass.
        assertEquals(0, new BigDecimal("1.00000").compareTo((BigDecimal) args.getValue()[11]));
        assertEquals(false, args.getValue()[5]);
    }

    private static AiEvaluationService service(JdbcTemplate jdbc, EssayGradingPort grader) {
        return service(jdbc, grader, 20);
    }

    private static AiEvaluationService service(JdbcTemplate jdbc, EssayGradingPort grader, int gradingTimeoutSeconds) {
        return new AiEvaluationService(jdbc, grader, mock(ActiveRubricProvider.class), new AssessmentLlmProperties(),
                new ObjectMapper(), 100, new BigDecimal("0.80"), new BigDecimal("0.60"), "Asia/Ho_Chi_Minh",
                new BigDecimal("0.05"), gradingTimeoutSeconds);
    }

    private static AiEvaluationService.GoldEssay gold(String essay, List<ExaminerErrorRecall.MarkedError> errors) {
        return new AiEvaluationService.GoldEssay(UUID.randomUUID(), "TASK_2", "prompt", null, essay,
                new BigDecimal("6.0"), new String[0], CRITERIA, errors);
    }
}
