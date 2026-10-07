package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.FirstAttemptMistakes;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.service.PracticeReviewRule;
import com.ieltspath.learning.domain.service.ReviewRule;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class SubmitPracticeAttemptUseCase {
    private static final int MIN_SET_QUESTIONS = 3;
    private final LearnerLock lock;
    private final PracticeAccess access;
    private final LearningContentClient content;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final KnowledgeEvidenceRepository evidence;
    private final WritingSubmissionRepository essays;
    private final PracticeProgress progress;
    private final FirstAttemptMistakes mistakes;
    private final ItemGrading grading = new ItemGrading();
    private final PracticeReviewRule reviewRule = new PracticeReviewRule();
    private final Clock clock = Clock.systemUTC();

    public SubmitPracticeAttemptUseCase(LearnerLock lock, PracticeAccess access, LearningContentClient content,
                                        PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                                        KnowledgeEvidenceRepository evidence, PracticeProgress progress,
                                        FirstAttemptMistakes mistakes, WritingSubmissionRepository essays) {
        this.lock = lock;
        this.access = access;
        this.content = content;
        this.attempts = attempts;
        this.reviews = reviews;
        this.evidence = evidence;
        this.progress = progress;
        this.mistakes = mistakes;
        this.essays = essays;
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
        var lesson = access.require(userId, attempt.lessonId());
        var version = content.getPackageVersion(attempt.packageVersionId());
        var graded = grading.grade(version, command.answers(), "Practice set", gradedEssays(userId, attempt, version));
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        boolean counted = !revealed.contains(attempt.packageId());
        List<KnowledgeEvidence> firstAnswers = new ArrayList<>();
        List<PracticeReviewRule.ItemOutcome> outcomes = new ArrayList<>();
        List<PracticeSubmission.Answer> results = new ArrayList<>();
        Map<LearningSkill, List<PracticeReviewRule.ItemOutcome>> outcomesBySkill = new EnumMap<>(LearningSkill.class);
        for (int index = 0; index < graded.items().size(); index++) {
            var item = graded.items().get(index);
            var grade = graded.grades().get(index);
            Set<UUID> kps = new LinkedHashSet<>();
            for (var mapping : item.knowledgePointMappings()) kps.add(mapping.knowledgePointId());
            var outcome = new PracticeReviewRule.ItemOutcome(kps, grade.correct());
            outcomes.add(outcome);
            LearningSkill itemSkill = graded.itemSkills().get(index);
            if (itemSkill != null) outcomesBySkill.computeIfAbsent(itemSkill, ignored -> new ArrayList<>()).add(outcome);
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
            var needs = reviewNeeds(graded, outcomes, outcomesBySkill, pending, availability);
            Set<UUID> wrongInLesson = needs.isEmpty() ? Set.of() : mistakes.of(userId, lesson);
            Map<UUID, LearningSkill> kpSkills = new HashMap<>();
            if (!needs.isEmpty()) evidence.findMasteryHistories(userId).stream().filter(history -> history.skill() != null)
                    .forEach(history -> kpSkills.put(history.knowledgePointId(), history.skill()));
            candidates = needs.stream().map(need -> {
                var start = ReviewRule.initialStage(need.kpPercent(), wrongInLesson.contains(need.knowledgePointId()));
                return new PracticeReviewCandidate(UUID.randomUUID(), attempt.lessonId(), need.knowledgePointId(),
                        kpSkills.getOrDefault(need.knowledgePointId(), attempt.skill()), attempt.id(), need.kpPercent(),
                        start.stage(), start.theoryReason());
            }).toList();
        }
        String transcript = ItemGrading.orderedSections(version).stream().filter(section -> section.audio() != null)
                .map(section -> section.audio().transcript()).findFirst().orElse(null);
        List<PracticeSubmission.ReviewCreated> created = candidates.stream()
                .map(candidate -> new PracticeSubmission.ReviewCreated(candidate.reviewId(),
                        candidate.knowledgePointId(), candidate.stage().name())).toList();
        PracticeSubmission response = new PracticeSubmission(attemptId, graded.correct(), graded.total(),
                graded.percent(), graded.passed(), counted, List.copyOf(results), transcript, created,
                graded.skillScores());
        attempt.recordResult(command.requestId(), response, clock.instant());
        attempts.saveResult(attempt);
        evidence.append(userId, firstAnswers);
        reviews.insertPracticePending(userId, candidates);
        progress.refreshPassForLesson(userId, attempt.lessonId());
        return response;
    }

    /**
     * Whether each essay of the set passed, from the newest submission of each; every essay must be GRADED first.
     */
    private Map<UUID, Boolean> gradedEssays(UUID userId, PracticeAttempt attempt, PackageVersion version) {
        var essayItems = ItemGrading.orderedItems(version).stream().filter(ItemGrading::isEssay).toList();
        if (essayItems.isEmpty()) return Map.of();
        var latest = essays.latestForPractice(userId, attempt.id());
        Map<UUID, Boolean> passed = new HashMap<>();
        for (var item : essayItems) {
            var submission = latest.get(item.questionVersionId());
            if (submission == null || submission.status() != WritingSubmissionStatus.GRADED) {
                throw new LearningRequestException(409, "ESSAY_NOT_GRADED",
                        "Submit every essay of the set and wait for its grade first");
            }
            passed.put(item.questionVersionId(), Boolean.TRUE.equals(submission.passed()));
        }
        return passed;
    }

    /**
     * Each objective skill below the pass mark is judged on its own items, so a weak Listening part of a mixed set
     * does not send its Reading knowledge points to review. Writing never creates reviews. A set whose sections name
     * no skill is judged as a whole.
     */
    private List<PracticeReviewRule.Need> reviewNeeds(ItemGrading.Graded graded,
                                                      List<PracticeReviewRule.ItemOutcome> outcomes,
                                                      Map<LearningSkill, List<PracticeReviewRule.ItemOutcome>> bySkill,
                                                      Set<UUID> pending, Map<UUID, Integer> availability) {
        if (graded.skillScores().isEmpty()) {
            return reviewRule.derive(graded.percent(), true, outcomes, pending, availability);
        }
        List<PracticeReviewRule.Need> needs = new ArrayList<>();
        Set<UUID> claimed = new HashSet<>(pending);
        for (var score : graded.skillScores()) {
            if (score.passed() || score.skill() == LearningSkill.WRITING) continue;
            var skillNeeds = reviewRule.derive(score.percent(), true, bySkill.get(score.skill()), claimed,
                    availability);
            skillNeeds.forEach(need -> claimed.add(need.knowledgePointId()));
            needs.addAll(skillNeeds);
        }
        return needs;
    }
}
