package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.PreparedStatementCreator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiEvaluationServiceTest {
    @Test void activationRejectsRunThatHasNotPassed() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        AiEvaluationService service = new AiEvaluationService(jdbc, mock(EssayGradingPort.class),
                mock(ActiveRubricProvider.class), new AssessmentLlmProperties(), new ObjectMapper(),
                100, new java.math.BigDecimal("0.80"), new java.math.BigDecimal("0.60"), "Asia/Ho_Chi_Minh");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activate(UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("EVALUATION_NOT_PASSED", error.getReason());
    }

    @Test void activationAcceptsPassedEvaluation() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        AiEvaluationService service = new AiEvaluationService(jdbc, mock(EssayGradingPort.class),
                mock(ActiveRubricProvider.class), new AssessmentLlmProperties(), new ObjectMapper(),
                100, new BigDecimal("0.80"), new BigDecimal("0.60"), "Asia/Ho_Chi_Minh");

        UUID runId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        assertDoesNotThrow(() -> service.activate(runId, actor));
        verify(jdbc).update(contains("status='DONE' AND passed IS TRUE"), eq(actor), eq(runId));
    }

    @Test void evaluationPersistsAggregateCriterionAndErrorCodeMetrics() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(any(PreparedStatementCreator.class))).thenReturn(1);
        EssayGradingPort grader = mock(EssayGradingPort.class);
        String criterionOne = "{\"summary\":\"ok\",\"criteria\":[{\"code\":\"TR\",\"band\":6.0,\"comment\":\"ok\"}],\"focus\":[]}";
        String criterionTwo = "{\"summary\":\"ok\",\"criteria\":[{\"code\":\"TR\",\"band\":6.5,\"comment\":\"ok\"}],\"focus\":[]}";
        when(grader.grade(any(), eq("essay one"))).thenReturn(new EssayGradingPort.EssayGrade(
                new BigDecimal("6.0"), criterionOne, "rubric@1+tplv1", List.of("GRAMMAR", "EXTRA")));
        when(grader.grade(any(), eq("essay two"))).thenReturn(new EssayGradingPort.EssayGrade(
                new BigDecimal("6.5"), criterionTwo, "rubric@1+tplv1", List.of()));
        AiEvaluationService service = new AiEvaluationService(jdbc, grader, mock(ActiveRubricProvider.class),
                new AssessmentLlmProperties(), new ObjectMapper(), 100, new BigDecimal("0.80"),
                new BigDecimal("0.60"), "Asia/Ho_Chi_Minh");
        List<AiEvaluationService.GoldEssay> gold = List.of(
                new AiEvaluationService.GoldEssay(UUID.randomUUID(), "TASK_2", "prompt", null, "essay one",
                        new BigDecimal("6.0"), new String[]{"GRAMMAR"}, "[{\"code\":\"TR\",\"band\":6.0,\"comment\":\"ok\"}]"),
                new AiEvaluationService.GoldEssay(UUID.randomUUID(), "TASK_2", "prompt", null, "essay two",
                        new BigDecimal("6.5"), new String[0], "[{\"code\":\"TR\",\"band\":6.5,\"comment\":\"ok\"}]"));
        UUID runId = UUID.randomUUID();

        service.evaluate(runId, "TASK_2", gold, "rubric@1+tplv1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(sql.capture(), args.capture());
        assertTrue(sql.getValue().contains("UPDATE ai_eval_runs SET prompt_version"));
        Object[] persisted = args.getValue();
        assertEquals("rubric@1+tplv1", persisted[0]);
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) persisted[1]));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) persisted[2]));
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) persisted[3]));
        assertEquals(0, new BigDecimal("0.5").compareTo((BigDecimal) persisted[4]));
        assertEquals(true, persisted[5]);
        assertTrue(persisted[6].toString().contains("\"TR\""));
        assertTrue(persisted[6].toString().contains("\"n\":2"));
        assertTrue(persisted[6].toString().contains("\"withinHalfBand\":1.000000"));
        assertEquals(runId, persisted[7]);
    }
}
