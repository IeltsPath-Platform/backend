package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.AnswerDurations;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.FirstAttemptMistakes;
import com.ieltspath.learning.application.service.FreePracticeSubmission;
import com.ieltspath.learning.application.service.GamificationRecorder;
import com.ieltspath.learning.application.service.HeartsService;
import com.ieltspath.learning.application.service.SpeakingPracticeBand;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingResultSignals;
import com.ieltspath.learning.application.service.WritingCriterionReviews;
import com.ieltspath.learning.application.service.ErrorTrapEvidence;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.GamificationSummary;
import com.ieltspath.learning.domain.vo.PracticeOrigin;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.PracticeAccess;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.service.LessonHintPolicy;
import com.ieltspath.learning.domain.service.PracticeReviewRule;
import com.ieltspath.learning.domain.service.SpeakingEvidenceRule;
import com.ieltspath.learning.domain.service.WritingEvidenceRule;
import com.ieltspath.learning.domain.service.ReviewRule;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeReviewCandidate;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import com.ieltspath.learning.domain.vo.PromotedReview;
import com.ieltspath.learning.domain.vo.PendingReview;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SubmitPracticeAttemptUseCase {
    private static final int MIN_SET_QUESTIONS = 3;
    private static final Set<String> SUGGESTION_TRIGGERS = Set.of("PRACTICE", "ERROR_PATTERN");
    private final LearnerLock lock;
    private final PracticeAccess access;
    private final LearningContentClient content;
    private final PracticeAttemptRepository attempts;
    private final ReviewItemRepository reviews;
    private final KnowledgeEvidenceRepository evidence;
    private final WritingSubmissionRepository essays;
    private final PracticeProgress progress;
    private final FirstAttemptMistakes mistakes;
    private final QuestionEventLog questionEvents;
    private final MasteryProfileUpdater masteryProfiles;
    private final FreePracticeSubmission freePractice;
    private final GamificationRecorder gamificationRecorder;
    private final HeartsService hearts;
    private final WritingCriterionReviews criterionReviews;
    private final ErrorTrapEvidence errorTrapEvidence;
    private final SpeakingSubmissionRepository speaking;
    private final SpeakingPracticeSettings speakingSettings;
    private final ItemGrading grading = new ItemGrading();
    private final LessonHintPolicy hints = new LessonHintPolicy();
    private final PracticeReviewRule reviewRule = new PracticeReviewRule();
    private final WritingEvidenceRule writingEvidence = new WritingEvidenceRule();
    private final SpeakingEvidenceRule speakingEvidence = new SpeakingEvidenceRule();
    private final Clock clock = Clock.systemUTC();

    @Autowired
    public SubmitPracticeAttemptUseCase(LearnerLock lock, PracticeAccess access, LearningContentClient content,
                                        PracticeAttemptRepository attempts, ReviewItemRepository reviews,
                                        KnowledgeEvidenceRepository evidence, PracticeProgress progress,
                                        FirstAttemptMistakes mistakes, WritingSubmissionRepository essays,
                                        QuestionEventLog questionEvents, MasteryProfileUpdater masteryProfiles,
                                        FreePracticeSubmission freePractice, GamificationRecorder gamificationRecorder,
                                        HeartsService hearts, WritingCriterionReviews criterionReviews,
                                        ErrorTrapEvidence errorTrapEvidence, SpeakingSubmissionRepository speaking,
                                        SpeakingPracticeSettings speakingSettings) {
        this.lock = lock;
        this.access = access;
        this.content = content;
        this.attempts = attempts;
        this.reviews = reviews;
        this.evidence = evidence;
        this.progress = progress;
        this.mistakes = mistakes;
        this.essays = essays;
        this.questionEvents = questionEvents;
        this.masteryProfiles = masteryProfiles;
        this.freePractice = freePractice;
        this.gamificationRecorder = gamificationRecorder;
        this.hearts = hearts;
        this.criterionReviews = criterionReviews;
        this.errorTrapEvidence = errorTrapEvidence;
        this.speaking = speaking;
        this.speakingSettings = speakingSettings;
    }

    /** Wrong objective answers; essay and recorded items are graded by someone else and never cost a heart. */
    static int wrongObjectiveAnswers(ItemGrading.Graded graded) {
        int wrong = 0;
        for (int index = 0; index < graded.items().size(); index++) {
            if (!graded.grades().get(index).correct() && !ItemGrading.isOpenAnswer(graded.items().get(index))) wrong++;
        }
        return wrong;
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
        // Free practice costs hearts too, so the gate sits before the branch that handles it.
        hearts.requireHearts(userId);
        if (attempt.origin() == PracticeOrigin.FREE) return freePractice.submit(userId, attempt, command);
        var lesson = access.require(userId, attempt.lessonId());
        var version = content.getPackageVersion(attempt.packageVersionId());
        Map<UUID, WritingSubmission> essaysByQuestion = gradedEssays(userId, attempt, version);
        Map<UUID, Boolean> essayPassed = new HashMap<>();
        essaysByQuestion.forEach((question, essay) -> essayPassed.put(question, Boolean.TRUE.equals(essay.passed())));
        Map<UUID, SpeakingSubmission> recordedByQuestion = gradedRecordings(userId, attempt, version);
        Map<UUID, Item> itemsByQuestion = new HashMap<>();
        ItemGrading.orderedItems(version).forEach(item -> itemsByQuestion.put(item.questionVersionId(), item));
        recordedByQuestion.forEach((question, answer) ->
                essayPassed.put(question, recordedPassed(itemsByQuestion.get(question), answer)));
        var graded = grading.grade(version, command.answers(), "Practice set", essayPassed);
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        boolean counted = !revealed.contains(attempt.packageId());
        List<KnowledgeEvidence> firstAnswers = new ArrayList<>();
        // Logged for every submission; a repeat of an already counted package is marked first_submission=false.
        Map<UUID, Long> durations = AnswerDurations.of(command.answers());
        List<QuestionAttemptEvent> events = new ArrayList<>();
        List<PracticeReviewRule.ItemOutcome> outcomes = new ArrayList<>();
        List<PracticeSubmission.Answer> results = new ArrayList<>();
        // Essay criterion points earn no milestone XP unless an item of this set also tests them.
        Set<UUID> itemKps = new HashSet<>();
        Set<UUID> criterionOnly = new HashSet<>();
        List<FailedWritingCriterion> failedCriteria = new ArrayList<>();
        List<FailedWritingCriterion> failedSpeaking = new ArrayList<>();
        ErrorTrapCatalog.ErrorTraps trapCatalog = errorTrapEvidence.loadIfNeeded(graded.items().stream().anyMatch(
                item -> item.answerSpec().get("distractors") instanceof Map<?, ?> || ItemGrading.isSpeaking(item)));
        List<String> feedbackCodes = graded.errorCodes().stream().filter(Objects::nonNull).toList();
        Map<String, com.ieltspath.learning.application.service.ErrorTypeTexts.Text> errorTexts =
                feedbackCodes.isEmpty() ? Map.of() : errorTrapEvidence.errorTexts(feedbackCodes);
        Map<LearningSkill, List<PracticeReviewRule.ItemOutcome>> outcomesBySkill = new EnumMap<>(LearningSkill.class);
        for (int index = 0; index < graded.items().size(); index++) {
            var item = graded.items().get(index);
            var grade = graded.grades().get(index);
            Set<UUID> kps = new LinkedHashSet<>();
            for (var mapping : item.knowledgePointMappings()) kps.add(mapping.knowledgePointId());
            var outcome = new PracticeReviewRule.ItemOutcome(kps, grade.correct());
            SpeakingSubmission recorded = recordedByQuestion.get(item.questionVersionId());
            // A recorded answer weighs on mastery only: its points are not sent to review when it falls short.
            if (recorded == null) outcomes.add(outcome);
            LearningSkill itemSkill = graded.itemSkills().get(index);
            if (itemSkill != null && recorded == null) {
                outcomesBySkill.computeIfAbsent(itemSkill, ignored -> new ArrayList<>()).add(outcome);
            }
            itemKps.addAll(kps);
            WritingSubmission essay = essaysByQuestion.get(item.questionVersionId());
            Map<UUID, Boolean> evidenceByKp = new LinkedHashMap<>();
            Set<UUID> spokenTraps = Set.of();
            String errorCode = graded.errorCodes().get(index);
            if (essay != null) {
                var essayOutcome = writingEvidence.derive(kps, essay.prompt().passBand(), grade.correct(), essay.grade());
                evidenceByKp.putAll(essayOutcome.correctByKnowledgePoint());
                criterionOnly.addAll(essayOutcome.criterionKnowledgePoints());
                if (counted) essayOutcome.failedCriteria().forEach(criterion ->
                        failedCriteria.add(criterion.fromSubmission(essay.id())));
                errorCode = essayOutcome.dominantErrorCode();
            } else if (recorded != null) {
                // Only a speech the grader could trust is evidence; otherwise the answer is shown but moves nothing.
                if (SpeakingResultSignals.usableAsEvidence(recorded)) {
                    var spoken = speakingEvidence.derive(kps, BigDecimal.valueOf(passBandOf(item)), grade.correct(),
                            SpeakingResultSignals.criteria(recorded), SpeakingResultSignals.errorCodes(recorded),
                            trapCatalog == null ? Map.of() : trapCatalog.trapKpByCode());
                    evidenceByKp.putAll(spoken.correctByKnowledgePoint());
                    criterionOnly.addAll(spoken.criterionKnowledgePoints());
                    spokenTraps = spoken.trapKnowledgePoints();
                    if (counted) failedSpeaking.addAll(spoken.failedCriteria());
                }
            } else {
                for (UUID kp : kps) evidenceByKp.put(kp, grade.correct());
            }
            EvidenceSource source = recorded != null ? EvidenceSource.LESSON_SPEAKING : EvidenceSource.PRACTICE_SET;
            if (counted) for (var entry : evidenceByKp.entrySet()) firstAnswers.add(KnowledgeEvidence.of(entry.getKey(),
                    entry.getValue(), source,
                    LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), entry.getKey()))
                    .forQuestion(item.questionVersionId()));
            // An error the grader marked in the recording is evidence against its trap point, as a marked error is in an essay.
            if (counted) for (UUID trap : spokenTraps) {
                firstAnswers.add(KnowledgeEvidence.of(trap, false, EvidenceSource.ERROR_TRAP,
                        LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), trap))
                        .forQuestion(item.questionVersionId()));
                criterionOnly.add(trap);
            }
            if (counted && recorded == null) errorTrapEvidence.append(firstAnswers, criterionOnly,
                    item.answerSpec(), grade.correct(), errorCode, item.questionVersionId(),
                    kp -> LessonEvidenceReference.forPracticeSet(command.requestId(), item.questionVersionId(), kp),
                    trapCatalog);
            events.add(QuestionAttemptEvent.of(userId, item.questionVersionId(), List.copyOf(kps), grade.correct(),
                    durations.get(item.questionVersionId()), errorCode,
                    QuestionEventSource.PRACTICE_SET, command.requestId(), counted));
            results.add(new PracticeSubmission.Answer(item.questionVersionId(), grade.correct(),
                    grade.correctAnswer(), item.explanation(),
                    hints.afterAnswer(grade.correct(), item.answerSpec(), item.options(), item.hint()),
                    errorTrapEvidence.practiceFeedback(errorCode, errorTexts)));
        }
        criterionOnly.removeAll(itemKps);
        List<PracticeReviewCandidate> candidates = List.of();
        List<PromotedReview> promoted = List.of();
        if (counted && !graded.passed()) {
            Set<UUID> kps = new HashSet<>();
            for (var item : outcomes) kps.addAll(item.knowledgePointIds());
            Set<UUID> excluded = new HashSet<>(revealed);
            excluded.add(attempt.packageId());
            Map<UUID, Integer> availability = kps.isEmpty() ? Map.of() : content.practiceSetAvailability(
                    kps.stream().sorted(Comparator.comparing(UUID::toString)).toList(),
                    excluded.stream().sorted(Comparator.comparing(UUID::toString)).toList(), MIN_SET_QUESTIONS);
            Set<UUID> blockingKps = new HashSet<>();
            Set<UUID> suggestionKps = new HashSet<>();
            Set<UUID> otherOptionalKps = new HashSet<>();
            for (PendingReview review : reviews.findPending(userId)) {
                if (review.blocking()) blockingKps.add(review.knowledgePointId());
                else if (SUGGESTION_TRIGGERS.contains(review.triggerKind())) suggestionKps.add(review.knowledgePointId());
                else otherOptionalKps.add(review.knowledgePointId());
            }
            var needs = reviewNeeds(graded, outcomes, outcomesBySkill, blockingKps, availability);
            // Weak again while the system's suggestion is still pending: that review now blocks.
            Set<UUID> repeatKps = needs.stream().map(PracticeReviewRule.Need::knowledgePointId)
                    .filter(suggestionKps::contains).collect(Collectors.toSet());
            promoted = reviews.promoteSuggestionsToBlocking(userId, repeatKps);
            // First weakness: a suggestion. A point holding a review the learner picked gets no second review.
            var fresh = needs.stream().filter(need -> !suggestionKps.contains(need.knowledgePointId())
                    && !otherOptionalKps.contains(need.knowledgePointId())).toList();
            Set<UUID> wrongInLesson = fresh.isEmpty() ? Set.of() : mistakes.of(userId, lesson);
            Map<UUID, LearningSkill> kpSkills = new HashMap<>();
            if (!fresh.isEmpty()) evidence.findMasteryHistories(userId).stream().filter(history -> history.skill() != null)
                    .forEach(history -> kpSkills.put(history.knowledgePointId(), history.skill()));
            candidates = fresh.stream().map(need -> {
                var start = ReviewRule.initialStage(need.kpPercent(), wrongInLesson.contains(need.knowledgePointId()));
                return new PracticeReviewCandidate(UUID.randomUUID(), attempt.lessonId(), need.knowledgePointId(),
                        kpSkills.getOrDefault(need.knowledgePointId(), attempt.skill()), attempt.id(), need.kpPercent(),
                        start.stage(), start.theoryReason(), false);
            }).toList();
        }
        String transcript = ItemGrading.orderedSections(version).stream().filter(section -> section.audio() != null)
                .map(section -> section.audio().transcript()).findFirst().orElse(null);
        List<PracticeSubmission.ReviewCreated> created = new ArrayList<>();
        candidates.forEach(candidate -> created.add(new PracticeSubmission.ReviewCreated(candidate.reviewId(),
                candidate.knowledgePointId(), candidate.stage().name(), false)));
        promoted.forEach(review -> created.add(new PracticeSubmission.ReviewCreated(review.reviewId(),
                review.knowledgePointId(), review.stage().name(), true)));
        // XP only for the first counted submission of a set, as evidence is.
        GamificationSummary xp = counted
                ? gamificationRecorder.recordCorrectAnswers(userId, command.requestId(), graded.correctQuestionIds())
                : gamificationRecorder.current(userId);
        int wrong = wrongObjectiveAnswers(graded);
        HeartsService.Snapshot heartState = hearts.lose(userId, wrong);
        GamificationSummary gamification = xp.withHearts(wrong, heartState.hearts(), heartState.nextHeartAt());
        PracticeSubmission response = new PracticeSubmission(attemptId, graded.correct(), graded.total(),
                graded.percent(), graded.passed(), counted, List.copyOf(results), transcript, created,
                graded.skillScores(), gamification);
        attempt.recordResult(command.requestId(), response, clock.instant());
        attempts.saveResult(attempt);
        questionEvents.append(events);
        evidence.append(userId, firstAnswers);
        masteryProfiles.recordEvidence(userId, firstAnswers, criterionOnly);
        if (counted) errorTrapEvidence.createReviews(userId, attempt.lessonId(), firstAnswers);
        reviews.insertPracticePending(userId, candidates);
        if (counted) criterionReviews.create(userId, attempt.lessonId(), failedCriteria);
        if (counted) criterionReviews.createForSpeaking(userId, attempt.lessonId(), failedSpeaking);
        progress.refreshPassForLesson(userId, attempt.lessonId());
        return response;
    }

    /**
     * The newest submission of each essay of the set, keyed by question; every essay must be GRADED first.
     */
    private Map<UUID, WritingSubmission> gradedEssays(UUID userId, PracticeAttempt attempt, PackageVersion version) {
        var essayItems = ItemGrading.orderedItems(version).stream().filter(ItemGrading::isEssay).toList();
        if (essayItems.isEmpty()) return Map.of();
        var latest = essays.latestForPractice(userId, attempt.id());
        Map<UUID, WritingSubmission> graded = new HashMap<>();
        for (var item : essayItems) {
            var submission = latest.get(item.questionVersionId());
            if (submission == null || submission.status() != WritingSubmissionStatus.GRADED) {
                throw new LearningRequestException(409, "ESSAY_NOT_GRADED",
                        "Submit every essay of the set and wait for its grade first");
            }
            graded.put(item.questionVersionId(), submission);
        }
        return graded;
    }

    /**
     * The recorded answer of the attempt, which must be GRADED before the attempt can be submitted. One submission
     * answers every recorded item of the set, so each of them must be one of its questions.
     */
    private Map<UUID, SpeakingSubmission> gradedRecordings(UUID userId, PracticeAttempt attempt, PackageVersion version) {
        var recordedItems = ItemGrading.orderedItems(version).stream().filter(ItemGrading::isSpeaking).toList();
        if (recordedItems.isEmpty()) return Map.of();
        SpeakingSubmission answer = speaking.findLatestForAttempt(userId, attempt.id())
                .filter(found -> found.status() == SpeakingSubmissionStatus.GRADED)
                .orElseThrow(() -> new LearningRequestException(409, "SPEAKING_NOT_GRADED",
                        "Record your answer and wait for its grade first"));
        Map<UUID, SpeakingSubmission> graded = new HashMap<>();
        for (var item : recordedItems) {
            if (answer.recordingFor(item.questionVersionId()).isEmpty()) {
                throw new LearningRequestException(409, "SPEAKING_NOT_GRADED",
                        "Record your answer and wait for its grade first");
            }
            graded.put(item.questionVersionId(), answer);
        }
        return graded;
    }

    /** Passes at the item's own band, or at the practice default when the item names none. */
    private boolean recordedPassed(Item item, SpeakingSubmission answer) {
        BigDecimal band = SpeakingPracticeBand.of(answer);
        return band != null && band.doubleValue() >= passBandOf(item);
    }

    /** The item's own pass band, or the practice default when the item names none. */
    private double passBandOf(Item item) {
        return item != null && item.answerSpec().get("passBand") instanceof Number band ? band.doubleValue()
                : speakingSettings.passBand();
    }

    /**
     * Each objective skill below the pass mark is judged on its own items, so a weak Listening part of a mixed set
     * does not send its Reading knowledge points to review. Writing criteria use optional reviews separately. A set whose sections name
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
            // A skill whose only items were recorded answers has no objective item to review.
            if (!bySkill.containsKey(score.skill())) continue;
            var skillNeeds = reviewRule.derive(score.percent(), true, bySkill.get(score.skill()), claimed,
                    availability);
            skillNeeds.forEach(need -> claimed.add(need.knowledgePointId()));
            needs.addAll(skillNeeds);
        }
        return needs;
    }
}
