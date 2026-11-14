package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.service.MasteryCalculator;
import com.ieltspath.learning.domain.service.MasteryPolicy;
import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The stored profile follows the evidence: tier with its margin, streak, review date and milestone XP. */
class MasteryProfileUpdaterTest {
    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private final UUID user = UUID.randomUUID();
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final MasteryProfileRepository profiles = mock(MasteryProfileRepository.class);
    private final QuestionEventLog events = mock(QuestionEventLog.class);
    private final LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
    private final GamificationRecorder gamification = mock(GamificationRecorder.class);
    private final BadgeService badges = mock(BadgeService.class);
    private final MasteryRulesProvider rules =
            new MasteryRulesProvider(configs, new ObjectMapper(), 0.6, 0.8, List.of(7, 14, 30, 45, 60), 3);

    private final Map<UUID, List<EvidencePoint>> points = new HashMap<>();
    private final List<MasteryHistory> histories = new ArrayList<>();
    private final Map<UUID, Instant> last = new HashMap<>();
    private final Map<UUID, MasteryProfile> previous = new HashMap<>();

    MasteryProfileUpdaterTest() {
        when(configs.findAll()).thenReturn(Map.of());
        when(evidence.findEvidencePoints(eq(user), anyCollection(), anyInt())).thenAnswer(call -> Map.copyOf(points));
        when(evidence.findMasteryHistories(eq(user), anyCollection())).thenAnswer(call -> List.copyOf(histories));
        when(evidence.lastEvidenceAt(eq(user), anyCollection())).thenAnswer(call -> Map.copyOf(last));
        when(events.statsByKnowledgePoint(eq(user), anyCollection(), any())).thenReturn(Map.of());
        when(profiles.find(eq(user), anyCollection())).thenAnswer(call -> Map.copyOf(previous));
    }

    /** MP-1.0 with a half-life so long that age changes nothing, or DeepTutor. */
    private MasteryProfileUpdater updater(boolean mp1) {
        MasteryFormulaSettings formulas = new MasteryFormulaSettings(configs, new ObjectMapper(), mp1 ? "MP1" : "DEEPTUTOR",
                8, 3, 1e12, 3, 2, 1.0, 0.03, 7, 1.0, 1.0, 1.0, 0.6, 0.5, 0.3, 0.75);
        return new MasteryProfileUpdater(evidence, profiles, events, rules, formulas, gamification, badges,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static EvidencePoint at(boolean correct, EvidenceSource source, Duration age) {
        return new EvidencePoint(correct, source, NOW.minus(age), null);
    }

    /** A point whose answers, oldest first, are the given ones. */
    private UUID point(List<EvidencePoint> answers) {
        UUID kp = UUID.randomUUID();
        points.put(kp, answers);
        histories.add(new MasteryHistory(kp, UUID.randomUUID(), false, answers.stream().map(EvidencePoint::correct).toList(),
                answers.size()));
        last.put(kp, NOW);
        return kp;
    }

    /** A DeepTutor point with the given outcomes and evidence count. */
    private UUID deepTutorPoint(List<Boolean> outcomes, long count) {
        UUID kp = UUID.randomUUID();
        histories.add(new MasteryHistory(kp, UUID.randomUUID(), false, outcomes, count));
        last.put(kp, NOW);
        return kp;
    }

    /** Wrong 4 days ago, then four right answers: 0.8 with no decay. */
    private static List<EvidencePoint> eightyPercent() {
        return List.of(at(false, EvidenceSource.REVIEW_SET, Duration.ofDays(4)),
                at(true, EvidenceSource.REVIEW_SET, Duration.ofDays(3)),
                at(true, EvidenceSource.REVIEW_SET, Duration.ofDays(2)),
                at(true, EvidenceSource.REVIEW_SET, Duration.ofDays(1)),
                at(true, EvidenceSource.REVIEW_SET, Duration.ofHours(1)));
    }

    /** An exam wrong, three exams right, a practice right: 3.6 / 4.6 = 0.783. */
    private static List<EvidencePoint> justUnderMaintain() {
        return List.of(at(false, EvidenceSource.ASSESSMENT, Duration.ofDays(4)),
                at(true, EvidenceSource.ASSESSMENT, Duration.ofDays(3)),
                at(true, EvidenceSource.ASSESSMENT, Duration.ofDays(2)),
                at(true, EvidenceSource.ASSESSMENT, Duration.ofDays(1)),
                at(true, EvidenceSource.PRACTICE_SET, Duration.ofHours(1)));
    }

    private MasteryProfile stored(UUID kp, double mastery, MasteryTier tier, int streak) {
        return new MasteryProfile(user, kp, mastery, tier, 5, streak, NOW.minus(Duration.ofDays(1)), null, null, List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, MasteryProfile> upserted() {
        ArgumentCaptor<List<MasteryProfile>> written = ArgumentCaptor.forClass(List.class);
        verify(profiles).upsert(written.capture());
        Map<UUID, MasteryProfile> byPoint = new HashMap<>();
        written.getValue().forEach(profile -> byPoint.put(profile.knowledgePointId(), profile));
        return byPoint;
    }

    @SuppressWarnings("unchecked")
    private List<GamificationRecorder.Rise> rises() {
        ArgumentCaptor<List<GamificationRecorder.Rise>> rises = ArgumentCaptor.forClass(List.class);
        verify(gamification).recordRises(eq(user), rises.capture());
        return rises.getValue();
    }

    private static KnowledgeEvidence practice(UUID kp) {
        return KnowledgeEvidence.of(kp, true, EvidenceSource.PRACTICE_SET, UUID.randomUUID());
    }

    @Test
    void aNewAnswerUnderMp1KeepsTheHeldTierInsideTheMarginAndGrowsTheStreak() {
        UUID kp = point(eightyPercent());
        previous.put(kp, stored(kp, 0.78, MasteryTier.CONSOLIDATE, 2));

        updater(true).recordEvidence(user, List.of(practice(kp)));

        MasteryProfile profile = upserted().get(kp);
        assertThat(profile.mastery()).isEqualTo(0.8);
        assertThat(profile.tier()).as("0.80 is past 0.80 but not by 0.03").isEqualTo(MasteryTier.CONSOLIDATE);
        assertThat(profile.successStreak()).isEqualTo(3);
        assertThat(profile.nextReviewAt()).isEqualTo(NOW.plus(Duration.ofDays(3)));
        assertThat(profile.confidence()).isNotNull();
    }

    @Test
    void theFirstProfileIsCutPlainly() {
        UUID kp = point(eightyPercent());

        updater(true).recordEvidence(user, List.of(practice(kp)));

        MasteryProfile profile = upserted().get(kp);
        assertThat(profile.tier()).isEqualTo(MasteryTier.MAINTAIN);
        assertThat(profile.successStreak()).isEqualTo(1);
    }

    @Test
    void underDeepTutorTheScoreComesFromTheOutcomesWithAPlainTier() {
        List<Boolean> outcomes = List.of(true, true, false, true, true);
        UUID kp = deepTutorPoint(outcomes, 5);

        updater(false).recordEvidence(user, List.of(practice(kp)));

        MasteryProfile profile = upserted().get(kp);
        double expected = MasteryPolicy.stored(new MasteryCalculator().compute(outcomes));
        assertThat(profile.mastery()).isEqualTo(expected);
        assertThat(profile.tier()).isEqualTo(new MasteryPolicy(rules.current()).tier(expected));
        assertThat(profile.confidence()).isNull();
        verify(evidence, never()).findEvidencePoints(any(), anyCollection(), anyInt());
    }

    @Test
    void practiceEvidenceThatReachesAJudgedTierEarnsMilestoneXpAndTheMaintainedBadge() {
        UUID kp = deepTutorPoint(List.of(true, true, true, true, true), 5);

        updater(false).recordEvidence(user, List.of(practice(kp)));

        assertThat(rises()).singleElement().satisfies(rise -> {
            assertThat(rise.knowledgePointId()).isEqualTo(kp);
            assertThat(rise.maintained()).isTrue();
        });
        verify(badges).onMaintained(user);
    }

    @Test
    void essayCriteriaTestsAndWeakPointsEarnNoMilestoneXp() {
        UUID criterion = deepTutorPoint(List.of(true, true, true, true, true), 5);
        UUID tested = deepTutorPoint(List.of(true, true, true, true, true), 5);
        UUID weak = deepTutorPoint(List.of(false, false, false, false, false), 5);

        updater(false).recordEvidence(user, List.of(practice(criterion),
                KnowledgeEvidence.assessment(tested, true, UUID.randomUUID(), UUID.randomUUID(), 1),
                KnowledgeEvidence.of(weak, false, EvidenceSource.PRACTICE_SET, UUID.randomUUID())), Set.of(criterion));

        assertThat(rises()).isEmpty();
        assertThat(upserted().get(weak).tier()).isEqualTo(MasteryTier.PRIORITY);
    }

    @Test
    void theMaintainedBadgeIsCheckedOnceForManyMaintainedPoints() {
        UUID first = deepTutorPoint(List.of(true, true, true, true, true), 5);
        UUID second = deepTutorPoint(List.of(true, true, true, true, true), 5);

        updater(false).recordEvidence(user, List.of(practice(first), practice(second)));

        verify(badges, times(1)).onMaintained(user);
    }

    @Test
    void aRebuildDropsTheMarginWhileANewAnswerKeepsIt() {
        UUID kp = point(justUnderMaintain());
        previous.put(kp, stored(kp, 0.79, MasteryTier.MAINTAIN, 1));

        updater(true).rebuild(user, List.of(kp));
        MasteryProfile rebuilt = upserted().get(kp);
        assertThat(rebuilt.mastery()).isEqualTo(0.783);
        assertThat(rebuilt.tier()).as("cut plainly").isEqualTo(MasteryTier.CONSOLIDATE);
        assertThat(rebuilt.successStreak()).as("the run of right answers at the end").isEqualTo(4);
        verify(gamification, never()).recordRises(any(), any());

        org.mockito.Mockito.clearInvocations(profiles);
        updater(true).recordEvidence(user, List.of(practice(kp)));
        assertThat(upserted().get(kp).tier()).as("held by the margin").isEqualTo(MasteryTier.MAINTAIN);
    }

    @Test
    void aRebuildOfAPointWithNoEvidenceLeftDeletesItsProfile() {
        UUID gone = UUID.randomUUID();

        updater(true).rebuild(user, List.of(gone));

        verify(profiles).delete(user, Set.of(gone));
    }

    @Test
    void decayUnderMp1KeepsTheStreakAndPaysNothing() {
        UUID kp = point(eightyPercent());
        previous.put(kp, stored(kp, 0.8, MasteryTier.MAINTAIN, 3));

        updater(true).decay(user, List.of(kp));

        MasteryProfile profile = upserted().get(kp);
        assertThat(profile.successStreak()).isEqualTo(3);
        assertThat(profile.tier()).isEqualTo(MasteryTier.MAINTAIN);
        verify(gamification, never()).recordRises(any(), any());
        verify(badges, never()).onMaintained(any());
    }

    @Test
    void decayUnderDeepTutorDoesNothing() {
        updater(false).decay(user, List.of(UUID.randomUUID()));
        verifyNoInteractions(evidence, profiles, events, gamification, badges);
    }

    @Test
    void noTouchedPointsReadsAndWritesNothing() {
        updater(true).recordEvidence(user, List.of());
        verifyNoInteractions(evidence, profiles, events, gamification, badges);
    }

    @Test
    void theEvidenceCountIsTheWholeHistoryNotTheWindow() {
        List<Boolean> twelve = List.of(true, true, true, true, true, true, true, true, true, true, true, true);
        UUID kp = deepTutorPoint(twelve, 12);

        updater(false).recordEvidence(user, List.of(practice(kp)));

        assertThat(upserted().get(kp).evidenceCount()).isEqualTo(12);
    }
}
