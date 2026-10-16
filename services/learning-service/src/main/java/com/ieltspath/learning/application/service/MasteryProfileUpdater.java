package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.port.QuestionEventLog.ErrorCount;
import com.ieltspath.learning.application.port.QuestionEventLog.EventStats;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.service.MasteryCalculator;
import com.ieltspath.learning.domain.service.MasteryPolicy;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.MasteryHistory;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
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
    private final Clock clock;
    private final MasteryCalculator calculator = new MasteryCalculator();

    @Autowired
    public MasteryProfileUpdater(KnowledgeEvidenceRepository evidence, MasteryProfileRepository profiles,
                                 QuestionEventLog events, MasteryRulesProvider rules) {
        this(evidence, profiles, events, rules, Clock.systemUTC());
    }

    MasteryProfileUpdater(KnowledgeEvidenceRepository evidence, MasteryProfileRepository profiles,
                          QuestionEventLog events, MasteryRulesProvider rules, Clock clock) {
        this.evidence = evidence;
        this.profiles = profiles;
        this.events = events;
        this.rules = rules;
        this.clock = clock;
    }

    /** Call right after {@code evidence.append}; the streak moves by the newest outcome of each point. */
    public void recordEvidence(UUID userId, List<KnowledgeEvidence> appended) {
        update(userId, appended.stream().map(KnowledgeEvidence::knowledgePointId).collect(Collectors.toSet()), false);
    }

    /**
     * Rebuilds the profiles of the given points from their evidence alone, for a regrade or the first fill. The streak
     * is the run of right answers at the end of the recent history, and a point left without evidence loses its profile.
     */
    public void rebuild(UUID userId, Collection<UUID> knowledgePointIds) {
        update(userId, new HashSet<>(knowledgePointIds), true);
    }

    private void update(UUID userId, Set<UUID> knowledgePointIds, boolean rebuild) {
        if (knowledgePointIds.isEmpty()) return;
        MasteryPolicy policy = new MasteryPolicy(rules.current());
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
            withEvidence.add(pointId);
            double mastery = MasteryPolicy.stored(calculator.compute(history.correctness()));
            MasteryTier tier = policy.tier(mastery);
            boolean lastCorrect = history.correctness().get(history.correctness().size() - 1);
            MasteryProfile before = previous.get(pointId);
            int streak = rebuild ? trailingRightAnswers(history.correctness())
                    : policy.nextStreak(before == null ? null : before.successStreak(),
                    before == null ? null : before.tier(), tier, lastCorrect);
            Instant last = lastEvidence.get(pointId);
            EventStats pointStats = stats.get(pointId);
            updated.add(new MasteryProfile(userId, pointId, mastery, tier, (int) history.evidenceCount(), streak, last,
                    policy.nextReviewAt(tier, last, streak),
                    pointStats == null ? null : pointStats.medianDurationMs(),
                    pointStats == null ? List.of() : pointStats.errors().stream().map(ErrorCount::code).toList()));
        }
        profiles.upsert(updated);
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
