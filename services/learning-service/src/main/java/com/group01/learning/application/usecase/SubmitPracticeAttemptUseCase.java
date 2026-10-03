package com.group01.learning.application.usecase;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.service.ItemGrading;
import com.group01.learning.application.service.LessonEvidenceReference;
import com.group01.learning.application.service.PracticeAccess;
import com.group01.learning.application.service.PracticeProgress;
import com.group01.learning.domain.aggregate.PracticeAttempt;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.PracticeAttemptRepository;
import com.group01.learning.domain.repository.ReviewItemRepository;
import com.group01.learning.domain.service.PracticeReviewRule;
import com.group01.learning.domain.vo.EvidenceSource;
import com.group01.learning.domain.vo.KnowledgeEvidence;
import com.group01.learning.domain.vo.PracticeReviewCandidate;
import com.group01.learning.domain.vo.PracticeSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
public class SubmitPracticeAttemptUseCase {
    private static final int MIN_SET_QUESTIONS = 3;
    private final LearnerLock lock;
    private final PracticeAccess access;
    private final LearningContentClient content;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final KnowledgeEvidenceRepository evidence;
    private final PracticeProgress progress;
    private final ItemGrading grading = new ItemGrading();
    private final PracticeReviewRule reviewRule = new PracticeReviewRule();
    private final Clock clock = Clock.systemUTC();

    public SubmitPracticeAttemptUseCase(LearnerLock lock, PracticeAccess access, LearningContentClient content,
                                        PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                                        KnowledgeEvidenceRepository evidence, PracticeProgress progress) {
        this.lock = lock;
        this.access = access;
        this.content = content;
        this.attempts = attempts;
        this.reviews = reviews;
        this.evidence = evidence;
        this.progress = progress;
    }

    @Transactional
    public PracticeSubmission execute(UUID userId, UUID attemptId, SubmitExerciseCommand command) {
        lock.lock(userId);
        PracticeAttempt attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (!attempt.acceptsAnswers(command.requestId())) return attempt.response();
        if (attempts.findByRequestId(command.requestId()).isPresent()) {
            throw new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
        }
        access.require(userId, attempt.lessonId());
        var version = content.getPackageVersion(attempt.packageVersionId());
        var graded = grading.grade(version, command.answers());
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        boolean counted = !revealed.contains(attempt.packageId());
        List<KnowledgeEvidence> firstAnswers = new ArrayList<>();
        List<PracticeReviewRule.ItemOutcome> outcomes = new ArrayList<>();
        List<PracticeSubmission.Answer> results = new ArrayList<>();
        for (int index = 0; index < graded.items().size(); index++) {
            var item = graded.items().get(index);
            var grade = graded.grades().get(index);
            Set<UUID> kps = new LinkedHashSet<>();
            for (var mapping : item.knowledgePointMappings()) kps.add(mapping.knowledgePointId());
            outcomes.add(new PracticeReviewRule.ItemOutcome(kps, grade.correct()));
            if (counted) for (UUID kp : kps) firstAnswers.add(KnowledgeEvidence.of(kp, grade.correct(),
                    EvidenceSource.PRACTICE_SET,
                    LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), kp)));
            results.add(new PracticeSubmission.Answer(item.questionVersionId(), grade.correct(),
                    grade.correctAnswer(), item.explanation()));
        }
        List<PracticeReviewCandidate> candidates = List.of();
        if (counted && !graded.passed()) {
            Set<UUID> kps = new HashSet<>();
            for (var item : outcomes) kps.addAll(item.knowledgePointIds());
            Set<UUID> excluded = new HashSet<>(revealed);
            excluded.add(attempt.packageId());
            Map<UUID, Integer> availability = kps.isEmpty() ? Map.of() : content.practiceSetAvailability(
                    kps.stream().sorted(Comparator.comparing(UUID::toString)).toList(),
                    excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList(), MIN_SET_QUESTIONS);
            Set<UUID> pending = new HashSet<>();
            reviews.findPending(userId).forEach(review -> pending.add(review.knowledgePointId()));
            candidates = reviewRule.derive(graded.percent(), true, outcomes, pending, availability).stream()
                    .map(need -> new PracticeReviewCandidate(UUID.randomUUID(), attempt.lessonId(),
                            need.knowledgePointId(), attempt.skill(), attempt.id(), need.kpPercent())).toList();
        }
        var first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        String transcript = first == null || first.audio() == null ? null : first.audio().transcript();
        List<PracticeSubmission.ReviewCreated> created = candidates.stream()
                .map(candidate -> new PracticeSubmission.ReviewCreated(candidate.reviewId(),
                        candidate.knowledgePointId(), "PRACTICE")).toList();
        PracticeSubmission response = new PracticeSubmission(attemptId, graded.correct(), graded.total(),
                graded.percent(), graded.passed(), counted, List.copyOf(results), transcript, created);
        attempt.recordResult(command.requestId(), response, clock.instant());
        attempts.saveResult(attempt);
        evidence.append(userId, firstAnswers);
        reviews.insertPracticePending(userId, candidates);
        progress.refreshPassForLesson(userId, attempt.lessonId());
        return response;
    }
}
