package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.PrerequisiteDiagnosis;
import com.ieltspath.learning.domain.service.PrerequisiteDiagnosis.ProbeFact;
import com.ieltspath.learning.domain.service.ReviewRule.ReviewCandidate;
import com.ieltspath.learning.domain.vo.DiagnosticProbe;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Prerequisite diagnosis for a blocking review that keeps failing or has no unseen set left. It opens a session of
 * short probes on the point's prerequisites, walks back through the ones that fail, and concludes with the gap points,
 * which get blocking reviews of their own. The caller holds the learner lock and the transaction.
 */
@Service
public class DiagnosticFlow {
    static final String DIAGNOSTIC_TRIGGER = "DIAGNOSTIC";
    static final int MIN_PROBE_QUESTIONS = 3;

    private final DiagnosticRepository diagnostics;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final CompletedLessonLocator lessonLocator;
    private final PrerequisiteDiagnosis rule = new PrerequisiteDiagnosis();

    public DiagnosticFlow(DiagnosticRepository diagnostics, ReviewItemRepository reviews,
                          PracticeAttemptRepository attempts, CompletedLessonLocator lessonLocator) {
        this.diagnostics = diagnostics;
        this.reviews = reviews;
        this.attempts = attempts;
        this.lessonLocator = lessonLocator;
    }

    public record Prepared(Map<UUID, List<UUID>> prerequisites,
                           Map<UUID, List<LearningContentClient.PracticeSet>> candidates,
                           Map<UUID, KnowledgePointDirectory.Entry> directory) {
        public Prepared {
            prerequisites = prerequisites == null ? Map.of() : Map.copyOf(prerequisites);
            candidates = candidates == null ? Map.of() : Map.copyOf(candidates);
            directory = directory == null ? Map.of() : Map.copyOf(directory);
        }
    }

    /** The review failed repeatedly. Content data must be prepared before the learner lock is acquired. */
    public Optional<DiagnosticSession> startAfterSecondFailure(ReviewItem review, Prepared prepared) {
        return start(review, prepared);
    }

    /** The review has no unseen set left, so the content team is told, and a diagnosis may follow. */
    public Optional<DiagnosticSession> startWhenOutOfSets(ReviewItem review, Prepared prepared) {
        if (!review.blocking()) return Optional.empty();
        diagnostics.recordContentGap(review.knowledgePointId());
        return start(review, prepared);
    }

    private Optional<DiagnosticSession> start(ReviewItem review, Prepared prepared) {
        // A review that came from a diagnosis is not diagnosed again, which keeps the walk back finite.
        if (!review.blocking() || DIAGNOSTIC_TRIGGER.equals(review.triggerKind())) return Optional.empty();
        if (diagnostics.findByRootReview(review.id()).isPresent()) return Optional.empty();
        UUID root = review.knowledgePointId();
        List<UUID> first = rule.nextProbes(0, prepared.prerequisites().getOrDefault(root, List.of()), Set.of(root), 0);

        Instant now = Instant.now();
        DiagnosticSession session = new DiagnosticSession(UUID.randomUUID(), review.userId(), review.id(), root,
                first.isEmpty() ? DiagnosticSession.Status.CONCLUDED : DiagnosticSession.Status.PROBING,
                List.of(), now, first.isEmpty() ? now : null);
        diagnostics.insertSession(session);
        if (!first.isEmpty()) {
            addProbes(session, root, 1, first, prepared);
            advance(session, prepared.directory());
        }
        return diagnostics.findSession(review.userId(), session.id());
    }

    /** A probe was answered: probe the prerequisites of a failed one, then conclude when none is left open. */
    public void afterAnswer(DiagnosticSession session, DiagnosticProbe answered, boolean passed, Prepared prepared) {
        if (!passed) {
            List<DiagnosticProbe> probes = diagnostics.findProbes(session.id());
            Set<UUID> known = new HashSet<>(probes.stream().map(DiagnosticProbe::knowledgePointId).toList());
            known.add(session.rootKnowledgePointId());
            List<UUID> next = rule.nextProbes(answered.depth(),
                    prepared.prerequisites().getOrDefault(answered.knowledgePointId(), List.of()), known,
                    probes.size());
            addProbes(session, answered.knowledgePointId(), answered.depth() + 1, next, prepared);
        }
        advance(session, prepared.directory());
    }

    /** An open probe whose set was seen elsewhere gets another set, or none, and the session moves on. */
    public void replaceStaleSet(DiagnosticSession session, DiagnosticProbe probe,
                                Optional<LearningContentClient.PracticeSet> next,
                                Map<UUID, KnowledgePointDirectory.Entry> described) {
        diagnostics.replaceProbeSet(probe.id(), next.map(LearningContentClient.PracticeSet::packageId).orElse(null),
                next.map(LearningContentClient.PracticeSet::packageVersionId).orElse(null));
        advance(session, described);
    }

    private void addProbes(DiagnosticSession session, UUID parent, int depth, List<UUID> knowledgePointIds,
                           Prepared prepared) {
        Set<UUID> taken = new HashSet<>();
        for (UUID kp : knowledgePointIds) {
            Optional<LearningContentClient.PracticeSet> set = prepared.candidates().getOrDefault(kp, List.of()).stream()
                    .filter(candidate -> !taken.contains(candidate.packageId()))
                    .filter(candidate -> !attempts.revealedPackageIds(session.userId()).contains(candidate.packageId()))
                    .filter(candidate -> !reviews.assignedPackageIds(session.userId()).contains(candidate.packageId()))
                    .findFirst();
            set.ifPresent(found -> taken.add(found.packageId()));
            diagnostics.insertProbe(DiagnosticProbe.open(session.id(), kp, parent, depth,
                    set.map(LearningContentClient.PracticeSet::packageId).orElse(null),
                    set.map(LearningContentClient.PracticeSet::packageVersionId).orElse(null)));
        }
    }

    private void advance(DiagnosticSession session, Map<UUID, KnowledgePointDirectory.Entry> described) {
        List<DiagnosticProbe> probes = diagnostics.findProbes(session.id());
        if (probes.stream().anyMatch(probe -> probe.status() == DiagnosticProbe.Status.OPEN)) return;
        conclude(session, probes, described);
    }

    private void conclude(DiagnosticSession session, List<DiagnosticProbe> probes,
                          Map<UUID, KnowledgePointDirectory.Entry> described) {
        List<ProbeFact> facts = probes.stream().map(probe -> new ProbeFact(probe.knowledgePointId(),
                probe.parentKnowledgePointId(), probe.depth(),
                probe.status() == DiagnosticProbe.Status.ANSWERED ? probe.passed() : null)).toList();
        PrerequisiteDiagnosis.Conclusion conclusion = rule.conclude(session.rootKnowledgePointId(), facts);
        diagnostics.conclude(session.id(), conclusion.knowledgePointIds());
        if (conclusion.rootIsGap()) return;

        UUID userId = session.userId();
        ReviewItem rootReview = reviews.findOwned(userId, session.rootReviewId()).orElseThrow();
        List<ReviewCandidate> candidates = conclusion.knowledgePointIds().stream()
                .map(kp -> new ReviewCandidate(kp, lessonLocator.teaching(userId, kp).orElse(rootReview.lessonId()),
                        skill(described.get(kp)))).toList();
        reviews.insertPending(userId, candidates, true, DIAGNOSTIC_TRIGGER);
        // A gap point that already had an optional review now blocks like the rest.
        reviews.promoteToBlocking(userId, conclusion.knowledgePointIds(), DIAGNOSTIC_TRIGGER);
        if (rootReview.status() != ReviewStatus.PENDING) return;
        List<UUID> gapReviews = reviews.findPending(userId).stream()
                .filter(pending -> conclusion.knowledgePointIds().contains(pending.knowledgePointId()))
                .map(PendingReview::reviewId).toList();
        if (!gapReviews.isEmpty()) diagnostics.waitOn(rootReview.id(), gapReviews);
    }

    private static LearningSkill skill(KnowledgePointDirectory.Entry entry) {
        if (entry == null || entry.skill() == null) return null;
        try {
            return LearningSkill.valueOf(entry.skill());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
