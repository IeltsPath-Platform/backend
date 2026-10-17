package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.port.QuestionEventLog.ErrorCount;
import com.ieltspath.learning.application.port.QuestionEventLog.EventStats;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.service.MasteryCalculator;
import com.ieltspath.learning.domain.service.MasteryPolicy;
import com.ieltspath.learning.domain.service.Mp1MasteryCalculator;
import com.ieltspath.learning.domain.vo.EvidencePoint;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import com.ieltspath.learning.domain.vo.EvidenceSource;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Keeps {@code kp_mastery_profiles} in step with the evidence. It runs inside the transaction that wrote the evidence,
 * for only the knowledge points that transaction touched.
 */
@Component
public class MasteryProfileUpdater {
    /** Time and error statistics come from this many recent days of events. */
    public static final Duration STATS_WINDOW = Duration.ofDays(90);

    private final KnowledgeEvidenceRepository evidence;
    private final MasteryProfileRepository profiles;
    private final QuestionEventLog events;
    private final MasteryRulesProvider rules;
    private final MasteryFormulaSettings formulas;
    private final GamificationRecorder gamification;
    private final BadgeService badges;
    private final Clock clock;
    private final MasteryCalculator calculator = new MasteryCalculator();

    /** How a profile is brought up to date: from new evidence, from scratch, or because time passed. */
    private enum Mode { RECORD, REBUILD, DECAY }

    @Autowired
    public MasteryProfileUpdater(KnowledgeEvidenceRepository evidence, MasteryProfileRepository profiles,
                                 QuestionEventLog events, MasteryRulesProvider rules,
                                 MasteryFormulaSettings formulas, GamificationRecorder gamification,
                                 BadgeService badges) {
        this(evidence, profiles, events, rules, formulas, gamification, badges, Clock.systemUTC());
    }

    MasteryProfileUpdater(KnowledgeEvidenceRepository evidence, MasteryProfileRepository profiles,
                          QuestionEventLog events, MasteryRulesProvider rules, MasteryFormulaSettings formulas,
                          GamificationRecorder gamification, BadgeService badges, Clock clock) {
        this.badges = badges;
        this.evidence = evidence;
        this.profiles = profiles;
        this.events = events;
        this.rules = rules;
        this.formulas = formulas;
        this.gamification = gamification;
        this.clock = clock;
    }

    /** Call right after {@code evidence.append}; the streak moves by the newest outcome of each point. */
    public void recordEvidence(UUID userId, List<KnowledgeEvidence> appended) {
        Set<UUID> touched = appended.stream().map(KnowledgeEvidence::knowledgePointId).collect(Collectors.toSet());
        // Milestone XP is paid only for practice evidence: tests and essays do not earn it.
        Set<UUID> practiced = appended.stream().filter(item -> PRACTICE_SOURCES.contains(item.source()))
                .map(KnowledgeEvidence::knowledgePointId).collect(Collectors.toSet());
        update(userId, touched, Mode.RECORD, practiced);
    }

    private static final Set<EvidenceSource> PRACTICE_SOURCES = EnumSet.of(EvidenceSource.LESSON_EXERCISE,
            EvidenceSource.PRACTICE_SET, EvidenceSource.REVIEW_SET, EvidenceSource.FREE_PRACTICE);

    /**
     * Rebuilds the profiles of the given points from their evidence alone, for a regrade or the first fill. The streak
     * is the run of right answers at the end of the recent history, and a point left without evidence loses its profile.
     */
    public void rebuild(UUID userId, Collection<UUID> knowledgePointIds) {
        update(userId, new HashSet<>(knowledgePointIds), Mode.REBUILD, Set.of());
    }

    /**
     * Recomputes the MP-1.0 score of points whose evidence aged: no new answer arrived, so the streak stays and the
     * tier is held by the hysteresis margin. Does nothing under the other formula, whose score never changes with time.
     */
    public void decay(UUID userId, Collection<UUID> knowledgePointIds) {
        if (!formulas.usesMp1()) return;
        update(userId, new HashSet<>(knowledgePointIds), Mode.DECAY, Set.of());
    }

    /** Evidence rows read per point for MP-1.0; far more than the window so repeats and capped practice are skipped. */
    private static final int MP1_EVIDENCE_READ = 60;

    /** {@code practiced} are the points whose new evidence came from practice; only they can earn milestone XP. */
    private void update(UUID userId, Set<UUID> knowledgePointIds, Mode mode, Set<UUID> practiced) {
        if (knowledgePointIds.isEmpty()) return;
        boolean rebuild = mode == Mode.REBUILD;
        boolean mp1 = formulas.usesMp1();
        Mp1MasteryCalculator mp1Calculator = new Mp1MasteryCalculator(formulas.params());
        MasteryPolicy policy = new MasteryPolicy(rules.current());
        Map<UUID, List<EvidencePoint>> points = mp1
                ? evidence.findEvidencePoints(userId, knowledgePointIds, MP1_EVIDENCE_READ) : Map.of();
        List<MasteryHistory> histories = evidence.findMasteryHistories(userId, knowledgePointIds);
        Map<UUID, Instant> lastEvidence = evidence.lastEvidenceAt(userId, knowledgePointIds);
        Map<UUID, EventStats> stats = events.statsByKnowledgePoint(userId, knowledgePointIds,
                clock.instant().minus(STATS_WINDOW));
        Map<UUID, MasteryProfile> previous = profiles.find(userId, knowledgePointIds);

        Set<UUID> withEvidence = new HashSet<>();
        List<MasteryProfile> updated = new java.util.ArrayList<>();
        for (MasteryHistory history : histories) {
            UUID pointId = history.knowledgePointId();
            if (history.evidenceCount() == 0 || history.correctness().isEmpty() || !lastEvidence.containsKey(pointId)) {
                continue;
            }
            MasteryProfile before = previous.get(pointId);
            List<EvidencePoint> pointEvidence = points.get(pointId);
            if (mp1 && (pointEvidence == null || pointEvidence.isEmpty())) continue;
            withEvidence.add(pointId);

            double mastery;
            MasteryTier tier;
            Double confidence = null;
            List<Boolean> outcomes;
            if (mp1) {
                var result = mp1Calculator.compute(pointEvidence, clock.instant());
                mastery = MasteryPolicy.stored(result.mastery());
                confidence = result.confidence();
                // A rebuild starts from scratch, so it is cut plainly; otherwise the held tier resists small moves.
                tier = policy.tier(mastery, result.sufficient(), rebuild || before == null ? null : before.tier(),
                        formulas.params().hysteresis());
                outcomes = pointEvidence.stream().map(EvidencePoint::correct).toList();
            } else {
                mastery = MasteryPolicy.stored(calculator.compute(history.correctness()));
                tier = policy.tier(mastery);
                outcomes = history.correctness();
            }
            boolean lastCorrect = outcomes.get(outcomes.size() - 1);
            int streak = switch (mode) {
                case REBUILD -> trailingRightAnswers(outcomes);
                case DECAY -> before == null ? trailingRightAnswers(outcomes) : before.successStreak();
                case RECORD -> policy.nextStreak(before == null ? null : before.successStreak(),
                        before == null ? null : before.tier(), tier, lastCorrect);
            };
            Instant last = lastEvidence.get(pointId);
            EventStats pointStats = stats.get(pointId);
            updated.add(new MasteryProfile(userId, pointId, mastery, tier, (int) history.evidenceCount(), streak, last,
                    policy.nextReviewAt(tier, last, streak),
                    pointStats == null ? null : pointStats.medianDurationMs(),
                    pointStats == null ? List.of() : pointStats.errors().stream().map(ErrorCount::code).toList(),
                    confidence));
        }
        profiles.upsert(updated);
        // Only live evidence pays milestone XP; a rebuild or decay re-derives scores that were already paid for.
        if (mode == Mode.RECORD) {
            List<GamificationRecorder.Rise> rises = updated.stream()
                    .filter(profile -> practiced.contains(profile.knowledgePointId()))
                    .filter(profile -> profile.tier() != MasteryTier.PRIORITY && profile.tier() != MasteryTier.INSUFFICIENT)
                    .map(profile -> new GamificationRecorder.Rise(profile.knowledgePointId(),
                            profile.tier() == MasteryTier.MAINTAIN))
                    .toList();
            gamification.recordRises(userId, rises);
            if (updated.stream().anyMatch(profile -> profile.tier() == MasteryTier.MAINTAIN)) {
                badges.onMaintained(userId);
            }
        }
        if (rebuild) {
            Set<UUID> gone = new HashSet<>(knowledgePointIds);
            gone.removeAll(withEvidence);
            profiles.delete(userId, gone);
        }
    }

    private static int trailingRightAnswers(List<Boolean> outcomes) {
        int run = 0;
        for (int index = outcomes.size() - 1; index >= 0 && outcomes.get(index); index--) run++;
        return run;
    }
}
