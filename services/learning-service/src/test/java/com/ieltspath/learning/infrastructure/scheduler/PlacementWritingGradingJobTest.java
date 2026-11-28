package com.ieltspath.learning.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.assessment.PlacementWritingGradingRequestedV1;
import com.ieltspath.learning.application.exception.WritingGradingException;
import com.ieltspath.learning.application.port.PlacementSpeakingJobStore;
import com.ieltspath.learning.application.port.PlacementWritingJobStore;
import com.ieltspath.learning.application.result.PlacementEssayGrade;
import com.ieltspath.learning.application.service.EssayGrader;
import com.ieltspath.learning.domain.vo.WritingGrade;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlacementWritingGradingJobTest {
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final PlacementWritingJobStore jobs = mock(PlacementWritingJobStore.class);
    private final EssayGrader grader = mock(EssayGrader.class);
    private final UUID eventId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @Test
    void keepsTheWholeGradeAndSendsAssessmentTheBandAndComments() throws Exception {
        claim(1);
        when(grader.available()).thenReturn(true);
        var grade = new WritingGrade(List.of(new WritingGrade.Criterion("TA", new BigDecimal("6.0"), List.of("Clear."),
                List.of("Add an overview."), null, List.of(), List.of(), null, null)), List.of(), "A fair answer.",
                new BigDecimal("6.0"), List.of(), null, "model", List.of(), List.of(), List.of(), null, null, List.of());
        when(grader.grade(any(), anyString())).thenReturn(new EssayGrader.GradedEssay(grade, "WT2@1+tpl3"));

        job().poll();

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(jobs).complete(eq(eventId), eq("GRADED"), isNull(), stored.capture(),
                argThat(event -> event.payload().contains("Clear. Add an overview.")
                        && event.payload().contains("WT2@1+tpl3")));
        // What is stored must read back, since the report is built from it.
        List<PlacementEssayGrade> essays = json.readValue(stored.getValue(), json.getTypeFactory()
                .constructCollectionType(List.class, PlacementEssayGrade.class));
        assertEquals(grade, essays.getFirst().grade());
        assertEquals(3, essays.getFirst().wordCount());
    }

    @Test
    void triesAgainThenTellsAssessmentTheEssayWasNotGraded() throws Exception {
        when(grader.available()).thenReturn(true);
        when(grader.grade(any(), anyString())).thenThrow(new WritingGradingException("INVALID_GRADE"));

        claim(1);
        job().poll();
        verify(jobs).retry(eq(eventId), eq("INVALID_GRADE"), any());
        verify(jobs, never()).complete(any(), anyString(), any(), any(), any());

        claim(2);
        job().poll();
        verify(jobs).complete(eq(eventId), eq("FAILED"), eq("INVALID_GRADE"), anyString(),
                argThat(event -> event.payload().contains("\"status\":\"FAILED\"")));
    }

    @Test
    void leavesAnEssayAssessmentAlreadyTookBackUngraded() throws Exception {
        claim(1, Instant.now().minusSeconds(600));

        job().poll();

        verify(grader, never()).grade(any(), anyString());
        verify(jobs).complete(eq(eventId), eq("FAILED"), eq("TOO_LATE"), anyString(),
                argThat(event -> event.payload().contains("TOO_LATE")));
    }

    private PlacementWritingGradingJob job() {
        return new PlacementWritingGradingJob(jobs, grader, json, 2, 5, 30, 300, 540);
    }

    private void claim(int attempts) throws Exception {
        claim(attempts, Instant.now());
    }

    private void claim(int attempts, Instant requestedAt) throws Exception {
        var request = new PlacementWritingGradingRequestedV1(eventId, PlacementWritingGradingRequestedV1.EVENT_TYPE,
                requestedAt, PlacementWritingGradingRequestedV1.SOURCE, new PlacementWritingGradingRequestedV1.Data(
                UUID.randomUUID(), UUID.randomUUID(), List.of(new PlacementWritingGradingRequestedV1.Item(itemId,
                UUID.randomUUID(), "Discuss.", "TASK_2", 250, new BigDecimal("6.0"), null, List.of(), "Cities are big."))));
        when(jobs.claimDue(anyInt(), anyInt(), any(), any())).thenReturn(List.of(new PlacementSpeakingJobStore.Job(
                eventId, request.data().attemptId(), request.data().userId(), json.writeValueAsString(request),
                attempts, "correlation", null)));
    }
}
