package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.application.port.PlacementSpeakingGradeStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.entity.SkillScore;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.repository.ItemResultRepository;
import com.ieltspath.assessment.domain.repository.SkillScoreRepository;
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
    @Mock SkillScoreRepository skillScores;
    @Mock AssessmentResultCompleter completer;
    @Mock GateEssayJobStore jobs;
    @Mock AutoGradeAttemptService autoGrader;
    @Mock PlacementSpeakingGradeStore speakingGrades;

    private final Map<UUID, String> skillBySection = new java.util.HashMap<>();
    private final UUID userId = UUID.randomUUID();
    private AssessmentAttempt attempt = attempt(null);
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

        // Listening 9.0, Reading 0.0 and Writing 0.0 are averaged; ungraded Speaking is omitted.
        assertThat(savedBand()).isEqualTo(3.0);
        assertThat(savedSkillBands()).containsOnlyKeys(Skill.LISTENING, Skill.READING, Skill.WRITING);
        assertThat(gradedItems()).extracting(ItemResult::correct).containsExactly(true, false, false, null);
        verify(jobs, never()).enqueueAi(any(), any());
        verify(completer).complete(eq(attempt), any(), eq(items), anyList());
    }

    @Test
    void configuredSpeakingBandIsIncludedInTheFourSkillAverage() {
        arrangeItems();
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());
        service(5.5).onSubmit(attempt);
        assertThat(savedBand()).isEqualTo(3.5);
        assertThat(savedSkillBands()).containsEntry(Skill.SPEAKING, 5.5);
    }

    @Test
    void disabledPlacementSpeakingGradingKeepsLegacyBandAndDoesNotReadRecordingJobs() {
        arrangeItems();
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());

        service(5.5,speakingGrades,false).onSubmit(attempt);

        assertThat(savedSkillBands()).containsEntry(Skill.SPEAKING,5.5);
        verifyNoInteractions(speakingGrades);
    }

    @Test
    void enabledPlacementSpeakingGradingDoesNotInventABandWhenTheLearnerSkipsRecording() {
        arrangeItems();
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());
        when(speakingGrades.recordings(attempt.getId(),userId)).thenReturn(List.of());

        service(5.5,speakingGrades,true).onSubmit(attempt);

        assertThat(savedSkillBands()).doesNotContainKey(Skill.SPEAKING);
    }

    @Test
    void selectedPoolCapsTheStoredBandAndReportsThatTheCeilingWasReached() {
        attempt = attempt(com.ieltspath.assessment.domain.vo.PlacementPool.BAND_0_3);
        arrangeItems(true);
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());
        service().onSubmit(attempt);
        AssessmentResult result = savedResult();
        assertThat(result.overallBand()).isEqualTo(3.0);
        assertThat(result.uncappedBand()).isGreaterThan(3.0);
        assertThat(result.placementPool()).isEqualTo(com.ieltspath.assessment.domain.vo.PlacementPool.BAND_0_3);
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

        // (9.0 + 0.0 + 7.0) / 3 = 5.5 after half-band rounding.
        assertThat(savedBand()).isEqualTo(5.5);
        assertThat(gradedItems()).extracting(ItemResult::correct).containsExactly(true, false, true, null);
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
    void anEssayJobWithoutBandIsNotGradedAndIsOmittedFromTheAverage() {
        arrangeItems();
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "COMPLETED", null)));

        service().completeIfGraded(attempt.getId());

        AssessmentResult result = savedResult();
        assertThat(result.overallBand()).isEqualTo(4.5);
        assertThat(result.partial()).isTrue();
    }

    @Test
    void aGradedTask1AndAnUngradedTask2LeaveWritingOutAltogether() {
        AttemptItem task1 = item("WRITING", essay("TASK_1"));
        AttemptItem task2 = item("WRITING", essay("TASK_2"));
        arrangeCustomItems(List.of(listening, reading, task1, task2));
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(task1.id(), "COMPLETED", new BigDecimal("6.0")),
                new GateEssayJobStore.JobState(task2.id(), "COMPLETED", null)));

        service().completeIfGraded(attempt.getId());

        // Task 1 alone must not stand in for Writing: Listening 9.0 and Reading 0.0 average to 4.5.
        assertThat(savedSkillBands()).containsOnlyKeys(Skill.LISTENING, Skill.READING);
        assertThat(savedResult().overallBand()).isEqualTo(4.5);
        assertThat(savedResult().partial()).isTrue();
    }

    @Test
    void anUnsentTask1DoesNotTurnAnUngradedTask2IntoAWritingBandOfZero() {
        AttemptItem task1 = item("WRITING", essay("TASK_1"));
        AttemptItem task2 = item("WRITING", essay("TASK_2"));
        arrangeCustomItems(List.of(listening, reading, task1, task2));
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(task2.id(), "COMPLETED", null)));

        service().completeIfGraded(attempt.getId());

        assertThat(savedSkillBands()).containsOnlyKeys(Skill.LISTENING, Skill.READING);
        assertThat(savedResult().partial()).isTrue();
        assertThat(gradedItems()).extracting(ItemResult::correct).containsExactly(true, false, false, null);
    }

    @Test
    void writingIsLabelledAsGradedByTheAiOnlyWhenAnEssayWasReallyGraded() {
        // No essay sent: the zero is computed, not graded.
        arrangeItems();
        when(jobs.submittedEssays(List.of(writing.id()))).thenReturn(Map.of());
        service().onSubmit(attempt);
        assertThat(savedSources()).containsEntry(Skill.WRITING, GradingSource.AUTO)
                .containsEntry(Skill.LISTENING, GradingSource.AUTO);
    }

    @Test
    void aGradedEssayMakesTheWritingSourceAi() {
        arrangeItems();
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(writing.id(), "COMPLETED", new BigDecimal("7.0"))));

        service().completeIfGraded(attempt.getId());

        assertThat(savedSources()).containsEntry(Skill.WRITING, GradingSource.AI);
    }

    @Test
    void whenEverySentEssayIsGradedWritingCountsAndTheResultIsNotPartial() {
        AttemptItem task1 = item("WRITING", essay("TASK_1"));
        AttemptItem task2 = item("WRITING", essay("TASK_2"));
        arrangeCustomItems(List.of(listening, reading, task1, task2));
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(task1.id(), "COMPLETED", new BigDecimal("5.0")),
                new GateEssayJobStore.JobState(task2.id(), "COMPLETED", new BigDecimal("6.5"))));

        service().completeIfGraded(attempt.getId());

        // (5.0 x 1 + 6.5 x 2) / 3 = 6.0
        assertThat(savedSkillBands()).containsEntry(Skill.WRITING, 6.0);
        assertThat(savedResult().partial()).isFalse();
    }

    @Test
    void skillBandsComeFromTheRowsSavedWhenTheResultWasGraded() {
        UUID resultId = UUID.randomUUID();
        when(skillScores.findByResultId(resultId)).thenReturn(List.of(
                new SkillScore(UUID.randomUUID(), resultId, Skill.LISTENING, null, 9.0, GradingSource.AUTO, null),
                new SkillScore(UUID.randomUUID(), resultId, Skill.READING, null, 0.0, GradingSource.AUTO, null),
                new SkillScore(UUID.randomUUID(), resultId, Skill.WRITING, null, 7.0, GradingSource.AI, null)));
        // These rows are immutable snapshots of the bands used at grading time.
        assertThat(service().skillBands(attempt.getId(), resultId)).containsExactly(
                entry(Skill.LISTENING, 9.0), entry(Skill.READING, 0.0), entry(Skill.WRITING, 7.0));
    }

    @Test
    void withoutAnEssayJobTheWritingSkillBandIsZero() {
        UUID resultId = UUID.randomUUID();
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of());

        assertThat(service().skillBands(attempt.getId(), resultId)).containsEntry(Skill.WRITING, 0.0)
                .containsEntry(Skill.LISTENING, 0.0).containsEntry(Skill.SPEAKING, 0.0);
    }

    @Test
    void legacyResultWithoutStoredSkillRowsStillIncludesTheFormerSpeakingZero() {
        UUID resultId = UUID.randomUUID();
        when(skillScores.findByResultId(resultId)).thenReturn(List.of());
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of());
        assertThat(service().skillBands(attempt.getId(), resultId)).containsEntry(Skill.SPEAKING, 0.0);
    }

    @Test
    void writingTask2WeighsTwiceTask1() {
        AttemptItem task1 = item("WRITING", "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_1\",\"passBand\":5.5},\"maxScore\":1.0}");
        AttemptItem task2 = item("WRITING", "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"TASK_2\",\"passBand\":5.5},\"maxScore\":1.0}");
        UUID resultId = UUID.randomUUID();
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(List.of(task1, task2));
        when(sections.findByAttemptId(attempt.getId())).thenReturn(List.of(task1, task2).stream()
                .map(item -> new AttemptSection(item.attemptSectionId(), attempt.getId(), UUID.randomUUID(), 0,
                        "{\"skill\":\"WRITING\"}")).toList());
        when(itemResults.findByResultId(resultId)).thenReturn(List.of());
        when(jobs.aiJobs(attempt.getId())).thenReturn(List.of(
                new GateEssayJobStore.JobState(task1.id(), "COMPLETED", new BigDecimal("5.0")),
                new GateEssayJobStore.JobState(task2.id(), "COMPLETED", new BigDecimal("6.5"))));

        // (5.0 x 1 + 6.5 x 2) / 3 = 6.0; a plain mean would give 5.75
        assertThat(service().skillBands(attempt.getId(), resultId)).containsExactly(entry(Skill.WRITING, 6.0));
    }

    private List<AttemptSection> sectionRows() {
        return items.stream().map(item -> new AttemptSection(item.attemptSectionId(), attempt.getId(),
                UUID.randomUUID(), 0, "{\"skill\":\"" + skillBySection.get(item.attemptSectionId()) + "\"}")).toList();
    }

    private static String essay(String task) {
        return "{\"answerSpec\":{\"type\":\"ESSAY\",\"task\":\"" + task + "\",\"passBand\":5.5},\"maxScore\":1.0}";
    }

    /** Arranges a completed-grading run over {@code custom}: Listening is correct, Reading wrong. */
    private void arrangeCustomItems(List<AttemptItem> custom) {
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(results.findLatestForUpdateByAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(custom);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(custom.stream()
                .map(item -> new AttemptSection(item.attemptSectionId(), attempt.getId(), UUID.randomUUID(), 0,
                        "{\"skill\":\"" + skillBySection.get(item.attemptSectionId()) + "\"}")).toList());
        when(responses.findByAttemptItemIds(anyList())).thenReturn(List.of());
        when(autoGrader.isCorrect(any(), any())).thenAnswer(invocation -> {
            AnswerSnapshot answer = invocation.getArgument(0);
            return "A".equals(answer.answerSpec().get("correct"));
        });
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void arrangeItems() {
        arrangeItems(false);
    }

    private void arrangeItems(boolean allCorrect) {
        when(attemptItems.findByAttemptId(attempt.getId())).thenReturn(items);
        when(sections.findByAttemptId(attempt.getId())).thenReturn(sectionRows());
        when(responses.findByAttemptItemIds(anyList())).thenReturn(List.of());
        when(autoGrader.isCorrect(any(), any())).thenAnswer(invocation -> {
            AnswerSnapshot answer = invocation.getArgument(0);
            return allCorrect || "A".equals(answer.answerSpec().get("correct"));
        });
        when(results.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private double savedBand() {
        ArgumentCaptor<AssessmentResult> captor = ArgumentCaptor.forClass(AssessmentResult.class);
        verify(results).save(captor.capture());
        return captor.getValue().overallBand();
    }

    private AssessmentResult savedResult() {
        ArgumentCaptor<AssessmentResult> captor = ArgumentCaptor.forClass(AssessmentResult.class);
        verify(results).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<Skill, Double> savedSkillBands() {
        ArgumentCaptor<List<SkillScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillScores).saveAll(captor.capture());
        return captor.getValue().stream().collect(java.util.stream.Collectors.toMap(SkillScore::skill, SkillScore::band));
    }

    @SuppressWarnings("unchecked")
    private Map<Skill, GradingSource> savedSources() {
        ArgumentCaptor<List<SkillScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillScores).saveAll(captor.capture());
        return captor.getValue().stream().collect(java.util.stream.Collectors.toMap(SkillScore::skill, SkillScore::gradingSource));
    }

    @SuppressWarnings("unchecked")
    private List<ItemResult> gradedItems() {
        ArgumentCaptor<List<ItemResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemResults).saveAll(captor.capture());
        return captor.getValue();
    }

    private PlacementGradingService service() {
        return service(null);
    }

    private PlacementGradingService service(Double configuredSpeakingBand) {
        return new PlacementGradingService(attempts, sections, attemptItems, responses, results, itemResults,
                completer, jobs, autoGrader, new ObjectMapper(), skillScores, configuredSpeakingBand, 5.5);
    }

    private PlacementGradingService service(Double configuredSpeakingBand,PlacementSpeakingGradeStore store,boolean enabled) {
        return new PlacementGradingService(attempts,sections,attemptItems,responses,results,itemResults,completer,jobs,
                autoGrader,new ObjectMapper(),skillScores,configuredSpeakingBand,5.5,store,enabled);
    }

    private AttemptItem item(String skill, String answerSnapshot) {
        UUID sectionId = UUID.randomUUID();
        skillBySection.put(sectionId, skill);
        return new AttemptItem(UUID.randomUUID(), sectionId, UUID.randomUUID(), 0, "{\"stem\":\"q\"}", answerSnapshot, null);
    }

    private AssessmentAttempt attempt(com.ieltspath.assessment.domain.vo.PlacementPool pool) {
        Instant now = Instant.now();
        return new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(), AttemptType.PLACEMENT,
                AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.SUBMITTED, now, now, null, 1, now, now,
                null, null, AttemptScope.FULL, null, pool);
    }
}
