package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerGoalClient;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The skill view, focus list, estimated band and target of the report, with the stores and the goal client mocked. */
class GetLearnerReportUseCaseTest {
    private static final BigDecimal B60 = new BigDecimal("6.0");

    private final UUID user = UUID.randomUUID();
    private final UUID readingA = UUID.randomUUID();
    private final UUID readingB = UUID.randomUUID();
    private final UUID writing = UUID.randomUUID();
    private final UUID listening = UUID.randomUUID();
    private final UUID grammar = UUID.randomUUID();

    private final LearnerGoalClient goals = mock(LearnerGoalClient.class);
    private GetLearnerReportUseCase useCase;

    @BeforeEach
    void wire() {
        MasteryProfileRepository profiles = mock(MasteryProfileRepository.class);
        QuestionEventLog events = mock(QuestionEventLog.class);
        ReviewItemRepository reviews = mock(ReviewItemRepository.class);
        KnowledgePointDirectory directory = mock(KnowledgePointDirectory.class);
        DiagnosticRepository diagnostics = mock(DiagnosticRepository.class);
        LearnerPlacementRepository placements = mock(LearnerPlacementRepository.class);

        when(profiles.findAll(user)).thenReturn(List.of(
                profile(readingA, 0.9, MasteryTier.MAINTAIN, 10), profile(readingB, 0.85, MasteryTier.MAINTAIN, 10),
                profile(writing, 0.2, MasteryTier.PRIORITY, 4), profile(listening, 0.3, MasteryTier.PRIORITY, 4),
                profile(grammar, 0.5, MasteryTier.CONSOLIDATE, 6)));
        when(directory.describe()).thenReturn(Map.of(
                readingA, new KnowledgePointDirectory.Entry("R1", "Reading 1", "READING", B60),
                readingB, new KnowledgePointDirectory.Entry("R2", "Reading 2", "READING", B60),
                writing, new KnowledgePointDirectory.Entry("W1", "Writing 1", "WRITING", B60),
                listening, new KnowledgePointDirectory.Entry("L1", "Listening 1", "LISTENING", B60),
                grammar, new KnowledgePointDirectory.Entry("G1", "Grammar 1", "GRAMMAR", B60)));
        when(events.statsByKnowledgePoint(any(), anyCollection(), any())).thenReturn(Map.of());
        when(events.recentByKnowledgePoint(any(), anyCollection(), anyInt())).thenReturn(Map.of());
        when(reviews.findPending(user)).thenReturn(List.of(
                new PendingReview(UUID.randomUUID(), UUID.randomUUID(), listening)));
        when(diagnostics.probingRootKnowledgePoints(user)).thenReturn(Set.of());
        when(placements.find(user)).thenReturn(Optional.empty());
        useCase = new GetLearnerReportUseCase(profiles, events, reviews, directory, diagnostics, placements, goals);
    }

    private MasteryProfile profile(UUID kp, double mastery, MasteryTier tier, int evidence) {
        return new MasteryProfile(user, kp, mastery, tier, evidence, 0, Instant.parse("2026-10-16T10:00:00Z"), null,
                null, List.of());
    }

    @Test
    void theSkillViewHasSixBlocksAndGroupsMasteryByEvidence() {
        when(goals.goal(user)).thenReturn(Optional.empty());

        LearnerReportResult report = useCase.execute(user);

        assertThat(report.skills()).extracting(LearnerReportResult.SkillSummary::skill)
                .containsExactly("LISTENING", "READING", "WRITING", "SPEAKING", "GRAMMAR", "VOCABULARY");
        LearnerReportResult.SkillSummary reading = report.skills().get(1);
        assertThat(reading.kpCount()).isEqualTo(2);
        assertThat(reading.tierCounts().get("MAINTAIN")).isEqualTo(2);
        assertThat(reading.mastery()).isCloseTo(0.875, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(reading.band()).isEqualByComparingTo(B60);
        assertThat(report.skills().get(3).mastery()).as("a skill with no points has no mastery").isNull();
    }

    @Test
    void focusListsTheWeakestUnfinishedPointsWithBlockedReviewsFirst() {
        when(goals.goal(user)).thenReturn(Optional.empty());

        LearnerReportResult report = useCase.execute(user);

        assertThat(report.focus()).extracting(LearnerReportResult.FocusPoint::code)
                .as("the point with a blocking review, then the lowest mastery").containsExactly("L1", "W1", "G1");
        assertThat(report.focus().get(0).reviewRequired()).isTrue();
    }

    @Test
    void theBandIsMarkedAsAnEstimateAndGrammarDoesNotCount() {
        when(goals.goal(user)).thenReturn(Optional.empty());

        LearnerReportResult.EstimatedBand band = useCase.execute(user).estimatedBand();

        assertThat(band.estimated()).isTrue();
        assertThat(band.value()).as("only reading has reached a band").isEqualByComparingTo(B60);
        assertThat(band.speakingIncluded()).isFalse();
    }

    @Test
    void theTargetCarriesTheGapToTheEstimatedBand() {
        when(goals.goal(user)).thenReturn(Optional.of(
                new LearnerGoalClient.Goal(new BigDecimal("7.0"), LocalDate.of(2027, 1, 20))));

        LearnerReportResult.Target target = useCase.execute(user).target();

        assertThat(target.targetBand()).isEqualByComparingTo("7.0");
        assertThat(target.gap()).isEqualByComparingTo("1.0");
        assertThat(target.examDate()).isEqualTo(LocalDate.of(2027, 1, 20));
        verify(goals, times(1)).goal(user);
    }

    @Test
    void withoutAGoalTheReportStillComesBackWithoutATarget() {
        when(goals.goal(user)).thenReturn(Optional.empty());

        LearnerReportResult report = useCase.execute(user);

        assertThat(report.target()).isNull();
        assertThat(report.knowledgePoints()).hasSize(5);
    }

    @Test
    void aGoalWithNothingSetLeavesTheTargetOut() {
        when(goals.goal(user)).thenReturn(Optional.of(new LearnerGoalClient.Goal(null, null)));

        assertThat(useCase.execute(user).target()).isNull();
    }
}
