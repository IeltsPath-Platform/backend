package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementGradingServiceTest {
    private static final String CORRECT_A = "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"A\"},\"maxScore\":1.0}";
    private static final String CORRECT_B = "{\"answerSpec\":{\"type\":\"CHOICE\",\"correct\":\"B\"},\"maxScore\":1.0}";
    private static final String ESSAY = "{\"answerSpec\":{\"type\":\"ESSAY\",\"passBand\":5.5},\"maxScore\":1.0}";
    private static final String SPEAKING = "{\"answerSpec\":{\"type\":\"SPEAKING\"},\"maxScore\":1.0}";

    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptSectionRepository sections;
    @Mock AttemptItemRepository attemptItems;
    @Mock AttemptResponseRepository responses;
    @Mock AssessmentResultRepository results;
    @Mock ItemResultRepository itemResults;
    @Mock AssessmentResultCompleter completer;
    @Mock GateEssayJobStore jobs;
    @Mock AutoGradeAttemptService autoGrader;

    private final Map<UUID, String> skillBySection = new java.util.HashMap<>();
    private final UUID userId = UUID.randomUUID();
    private final AssessmentAttempt attempt = attempt();
    private final AttemptItem listening = item("LISTENING", CORRECT_A);
    private final AttemptItem reading = item("READING", CORRECT_B);
    private final AttemptItem writing = item("WRITING", ESSAY);
    private final AttemptItem speaking = item("SPEAKING", SPEAKING);
    private final List<AttemptItem> items = List.of(listening, reading, writing, speaking);

    @Test
    void withoutASentEssayTheAttemptIsGradedAtOnceAndWritingScoresZero() {
        arrangeItems();
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());

        service().onSubmit(attempt);

        // listening 100% -> 7.0, reading 0% -> 4.0, writing none -> 0.0, speaking fixed 5.5; mean 4.125 -> 4.0
        assertThat(savedBand()).isEqualTo(4.0);
        assertThat(gradedItems()).extracting(ItemResult::correct).containsExactly(true, false, false, true);
        verify(jobs, never()).enqueueAi(any(), any());
        verify(completer).complete(eq(attempt), any(), eq(items), anyList());
    }

    @Test
    void aSentEssayIsQueuedForTheLlmAndNothingIsCompletedYet() {
        UUID submission = UUID.randomUUID();
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of(writing.id(), submission));

        service().onSubmit(attempt);

        verify(jobs).enqueueAi(Map.of(writing.id(), submission), userId);
        verifyNoInteractions(results, completer);
    }

    @Test
    void completedLlmBandFeedsTheWritingSkillBand() {
        arrangeItems();
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "COMPLETED", new BigDecimal("7.0"))));

        assertThat(service().completeIfGraded(attempt.getId())).isTrue();

        // (7.0 + 4.0 + 7.0 + 5.5) / 4 = 5.875 -> 6.0
        assertThat(savedBand()).isEqualTo(6.0);
        assertThat(gradedItems()).extracting(ItemResult::correct).containsExactly(true, false, true, true);
    }

    @Test
    void waitsWhileAnEssayJobIsStillOpenAndNeverCompletesTwice() {
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "QUEUED", null)));

        assertThat(service().completeIfGraded(attempt.getId())).isFalse();

        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.of(
                new AssessmentResult(UUID.randomUUID(), attempt.getId(), 1, AssessmentResult.COMPLETED, 5.0, Instant.now())));
        assertThat(service().completeIfGraded(attempt.getId())).isFalse();
        verifyNoInteractions(completer);
    }

    @Test
    void anEssayJobWithoutBandTakesTheDefaultWritingBand() {
        arrangeItems();
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "COMPLETED", null)));

        service().completeIfGraded(attempt.getId());

        // default writing 5.5: (7.0 + 4.0 + 5.5 + 5.5) / 4 = 5.5
        assertThat(savedBand()).isEqualTo(5.5);
    }

    @Test
    void skillBandsAreRecomputedFromTheStoredResultTheSameWayTheOverallBandWas() {
        UUID resultId = UUID.randomUUID();
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of(
                new ItemResult(UUID.randomUUID(), resultId, listening.id(), 1.0, 1.0, true, null, "{}"),
                new ItemResult(UUID.randomUUID(), resultId, reading.id(), 0.0, 1.0, false, null, "{}")));
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "COMPLETED", new BigDecimal("7.0"))));

        // Same inputs as completedLlmBandFeedsTheWritingSkillBand, whose overall band is 6.0.
        assertThat(service().skillBands(attempt.getId(), resultId)).containsExactly(
                entry(Skill.LISTENING, 7.0), entry(Skill.READING, 4.0),
                entry(Skill.WRITING, 7.0), entry(Skill.SPEAKING, 5.5));
    }

    @Test
    void withoutAnEssayJobTheWritingSkillBandIsZero() {
        UUID resultId = UUID.randomUUID();
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of());

        assertThat(service().skillBands(attempt.getId(), resultId)).containsEntry(Skill.WRITING, 0.0)
                .containsEntry(Skill.LISTENING, 4.0);
    }

    private List<AttemptSection> sectionRows() {
        return items.stream().map(item -> new AttemptSection(item.attemptSectionId(), attempt.getId(),
                UUID.randomUUID(), 0, "{\"skill\":\"" + skillBySection.get(item.attemptSectionId()) + "\"}")).toList();
    }

    private void arrangeItems() {
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(responses.findByAttemptItemIds(anyList())).thenReturn(List.of());
        when(autoGrader.isCorrect(any(), any())).thenAnswer(invocation -> {
            AnswerSnapshot answer = invocation.getArgument(0);
            return "A".equals(answer.answerSpec().get("correct"));
        });
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private double savedBand() {
        ArgumentCaptor<AssessmentResult> captor = ArgumentCaptor.forClass(AssessmentResult.class);
        verify(results).save(captor.capture());
        return captor.getValue().overallBand();
    }

    @SuppressWarnings("unchecked")
    private List<ItemResult> gradedItems() {
        ArgumentCaptor<List<ItemResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemResults).saveAll(captor.capture());
        return captor.getValue();
    }

    private PlacementGradingService service() {
        return new PlacementGradingService(attempts, sections, attemptItems, responses, results, itemResults,
                completer, jobs, autoGrader, new ObjectMapper(), 5.5, 5.5);
    }

    private AttemptItem item(String skill, String answerSnapshot) {
        UUID sectionId = UUID.randomUUID();
        skillBySection.put(sectionId, skill);
        return new AttemptItem(UUID.randomUUID(), sectionId, UUID.randomUUID(), 0, "{\"stem\":\"q\"}", answerSnapshot, null);
    }

    private AssessmentAttempt attempt() {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), AttemptType.PLACEMENT,
                AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now);
    }
}
