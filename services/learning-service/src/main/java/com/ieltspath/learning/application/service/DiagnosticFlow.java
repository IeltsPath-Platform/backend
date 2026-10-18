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

    private final LearningContentClient content;
    private final DiagnosticRepository diagnostics;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final CompletedLessonLocator lessonLocator;
    private final KnowledgePointDirectory directory;
    private final PrerequisiteDiagnosis rule = new PrerequisiteDiagnosis();

    public DiagnosticFlow(LearningContentClient content, DiagnosticRepository diagnostics,
                          ReviewItemRepository reviews, PracticeAttemptRepository attempts,
                          CompletedLessonLocator lessonLocator, KnowledgePointDirectory directory) {
        this.content = content;
        this.diagnostics = diagnostics;
        this.reviews = reviews;
        this.attempts = attempts;
        this.lessonLocator = lessonLocator;
        this.directory = directory;
    }

    /** The review failed its second set. */
    public Optional<DiagnosticSession> startAfterSecondFailure(ReviewItem review) {
        return start(review);
    }

    /**
     * The review has no unseen set left, so the content team is told, and a diagnosis may follow. Call before the review
     * is skipped.
     */
    public Optional<DiagnosticSession> startWhenOutOfSets(ReviewItem review) {
        if (!review.blocking()) return Optional.empty();
        diagnostics.recordContentGap(review.knowledgePointId());
        return start(review);
    }

    private Optional<DiagnosticSession> start(ReviewItem review) {
        // A review that came from a diagnosis is not diagnosed again, which keeps the walk back finite.
        if (!review.blocking() || DIAGNOSTIC_TRIGGER.equals(review.triggerKind())) return Optional.empty();
        if (diagnostics.findByRootReview(review.id()).isPresent()) return Optional.empty();
        UUID root = review.knowledgePointId();
        List<UUID> first = rule.nextProbes(0, prerequisitesOf(root), Set.of(root), 0);
        if (first.isEmpty()) return Optional.empty();

        DiagnosticSession session = new DiagnosticSession(UUID.randomUUID(), review.userId(), review.id(), root,
                DiagnosticSession.Status.PROBING, List.of(), null, null);
        diagnostics.insertSession(session);
        addProbes(session, root, 1, first);
        advance(session);
        return diagnostics.findSession(review.userId(), session.id());
    }

    /** A probe was answered: probe the prerequisites of a failed one, then conclude when none is left open. */
    public void afterAnswer(DiagnosticSession session, DiagnosticProbe answered, boolean passed) {
        if (!passed) {
            List<DiagnosticProbe> probes = diagnostics.findProbes(session.id());
            Set<UUID> known = new HashSet<>(probes.stream().map(DiagnosticProbe::knowledgePointId).toList());
            known.add(session.rootKnowledgePointId());
            List<UUID> next = rule.nextProbes(answered.depth(), prerequisitesOf(answered.knowledgePointId()), known,
                    probes.size());
            addProbes(session, answered.knowledgePointId(), answered.depth() + 1, next);
        }
        advance(session);
    }

    /** An open probe whose set was seen elsewhere gets another set, or none, and the session moves on. */
    public void replaceStaleSet(DiagnosticSession session, DiagnosticProbe probe) {
        Optional<LearningContentClient.PracticeSet> next = unseenSet(session.userId(), probe.knowledgePointId(),
                new HashSet<>());
        diagnostics.replaceProbeSet(probe.id(), next.map(LearningContentClient.PracticeSet::packageId).orElse(null),
                next.map(LearningContentClient.PracticeSet::packageVersionId).orElse(null));
        advance(session);
    }

    private void addProbes(DiagnosticSession session, UUID parent, int depth, List<UUID> knowledgePointIds) {
        Set<UUID> taken = new HashSet<>();
        for (UUID kp : knowledgePointIds) {
            Optional<LearningContentClient.PracticeSet> set = unseenSet(session.userId(), kp, taken);
            set.ifPresent(found -> taken.add(found.packageId()));
            diagnostics.insertProbe(DiagnosticProbe.open(session.id(), kp, parent, depth,
                    set.map(LearningContentClient.PracticeSet::packageId).orElse(null),
                    set.map(LearningContentClient.PracticeSet::packageVersionId).orElse(null)));
        }
    }

    /** A set of at least {@value #MIN_PROBE_QUESTIONS} questions whose answers the learner has not seen. */
    private Optional<LearningContentClient.PracticeSet> unseenSet(UUID userId, UUID kp, Set<UUID> alsoTaken) {
        Set<UUID> excluded = new HashSet<>(attempts.revealedPackageIds(userId));
        excluded.addAll(reviews.assignedPackageIds(userId));
        excluded.addAll(alsoTaken);
        return content.searchPracticeSets(kp, new ArrayList<>(excluded), MIN_PROBE_QUESTIONS, 1).stream().findFirst();
    }

    private List<UUID> prerequisitesOf(UUID kp) {
        Map<UUID, List<UUID>> prerequisites = content.prerequisites(List.of(kp));
        List<UUID> found = prerequisites == null ? null : prerequisites.get(kp);
        return found == null ? List.of() : found;
    }

    private void advance(DiagnosticSession session) {
        List<DiagnosticProbe> probes = diagnostics.findProbes(session.id());
        if (probes.stream().anyMatch(probe -> probe.status() == DiagnosticProbe.Status.OPEN)) return;
        conclude(session, probes);
    }

    private void conclude(DiagnosticSession session, List<DiagnosticProbe> probes) {
        List<ProbeFact> facts = probes.stream().map(probe -> new ProbeFact(probe.knowledgePointId(),
                probe.parentKnowledgePointId(), probe.depth(),
                probe.status() == DiagnosticProbe.Status.ANSWERED ? probe.passed() : null)).toList();
        PrerequisiteDiagnosis.Conclusion conclusion = rule.conclude(session.rootKnowledgePointId(), facts);
        diagnostics.conclude(session.id(), conclusion.knowledgePointIds());
        if (conclusion.rootIsGap()) return;

        UUID userId = session.userId();
        ReviewItem rootReview = reviews.findOwned(userId, session.rootReviewId()).orElseThrow();
        Map<UUID, KnowledgePointDirectory.Entry> described = directory.describe();
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
