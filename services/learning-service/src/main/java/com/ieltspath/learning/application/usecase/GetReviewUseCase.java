package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.PracticeSet;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.result.LessonResult;
import com.ieltspath.learning.application.result.ReviewResult;
import com.ieltspath.learning.application.result.ErrorTrapResult;
import com.ieltspath.learning.application.service.DiagnosticFlow;
import com.ieltspath.learning.application.service.FirstAttemptMistakes;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.vo.DiagnosticSession;
import com.ieltspath.learning.application.service.ItemGrading;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.application.service.TheoryFocus;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.ErrorTypeTexts;
import com.ieltspath.learning.application.service.DiagnosticPreparation;
import com.ieltspath.learning.application.service.ReviewSettings;
import com.ieltspath.learning.domain.aggregate.ReviewItem;
import com.ieltspath.learning.domain.entity.ReviewSet;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.service.MainErrorRule;
import com.ieltspath.learning.domain.vo.ReviewStage;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import com.ieltspath.learning.domain.vo.TheoryReason;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A pending review shows the theory of its knowledge point and, at the practice stage, one set of a package whose
 * answers the learner has never seen; at the theory stage it shows a quick check instead of a set. The review's own
 * rules ({@link ReviewItem}) decide DONE, the theory step, or SKIPPED.
 */
@Service
public class GetReviewUseCase {
    private static final int MIN_SET_QUESTIONS = 3;

    private final LearningContentClient content;
    private final LearnerLock lock;
    private final ReviewItemRepository reviews;
    private final PracticeAttemptRepository attempts;
    private final PracticeProgress practice;
    private final FirstAttemptMistakes mistakes;
    private final DiagnosticRepository diagnostics;
    private final DiagnosticFlow diagnosticFlow;
    private final ErrorTrapCatalog errorTraps;
    private final MasteryProfileRepository profiles;
    private final ErrorTypeTexts errorTypeTexts;
    private final DiagnosticPreparation diagnosticPreparation;
    private final ReviewSettings reviewSettings;
    private final TransactionTemplate transaction;
    private final TheoryFocus theoryFocus = new TheoryFocus();

    @Autowired
    public GetReviewUseCase(LearningContentClient content, LearnerLock lock, ReviewItemRepository reviews,
                            PracticeAttemptRepository attempts, PracticeProgress practice,
                            FirstAttemptMistakes mistakes, DiagnosticRepository diagnostics,
                            DiagnosticFlow diagnosticFlow, ErrorTrapCatalog errorTraps,
                            MasteryProfileRepository profiles, ErrorTypeTexts errorTypeTexts,
                            DiagnosticPreparation diagnosticPreparation, ReviewSettings reviewSettings,
                            PlatformTransactionManager transactionManager) {
        this.reviewSettings = reviewSettings;
        this.diagnostics = diagnostics;
        this.diagnosticFlow = diagnosticFlow;
        this.errorTraps = errorTraps;
        this.profiles = profiles;
        this.errorTypeTexts = errorTypeTexts;
        this.diagnosticPreparation = diagnosticPreparation;
        this.transaction = new TransactionTemplate(transactionManager);
        this.content = content;
        this.lock = lock;
        this.reviews = reviews;
        this.attempts = attempts;
        this.practice = practice;
        this.mistakes = mistakes;
    }

    public ReviewResult execute(UUID userId, UUID reviewId) {
        for (int retry = 0; retry < 2; retry++) {
            ReviewItem snapshot = ownedReview(userId, reviewId);
            Prepared prepared = prepare(userId, snapshot);
            ReadResult read = transaction.execute(status -> readUnderLock(userId, reviewId, prepared));
            if (!read.retry()) return read.result();
        }
        throw new LearningRequestException(409, "REVIEW_SET_CLOSED", "Review changed while it was being opened");
    }

    private Prepared prepare(UUID userId, ReviewItem review) {
        Lesson lesson = null;
        try {
            lesson = content.getLesson(review.lessonId());
        } catch (LearningRequestException error) {
            if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
        }
        TheoryFocus.Focus focus = lesson == null
                ? new TheoryFocus.Focus(List.of(), "LESSON_FALLBACK", List.of())
                : theoryFocus.of(lesson, review.knowledgePointId());
        boolean diagnosticWithoutLesson = "DIAGNOSTIC".equals(review.triggerKind())
                && "LESSON_FALLBACK".equals(focus.scope());
        if (diagnosticWithoutLesson) focus = new TheoryFocus.Focus(List.of(), "NO_LESSON", List.of());
        ErrorTrapResult trap = review.status() == ReviewStatus.PENDING
                ? errorTrap(userId, review.knowledgePointId()) : null;
        Optional<DiagnosticSession> diagnosis = diagnostics.findByRootReview(review.id());
        PackageVersion currentVersion = null;
        boolean staleOpenSet = false;
        Optional<PracticeSet> next = Optional.empty();
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        boolean archivedTheory = review.stage() == ReviewStage.THEORY && (lesson == null || diagnosticWithoutLesson);
        if (review.status() == ReviewStatus.PENDING && (review.stage() != ReviewStage.THEORY || archivedTheory)) {
            if (review.openSet().isPresent()) {
                ReviewSet open = review.openSet().orElseThrow();
                if (revealed.contains(open.packageId())) {
                    staleOpenSet = true;
                } else {
                    try {
                        currentVersion = content.getPackageVersion(open.packageVersionId());
                    } catch (LearningRequestException error) {
                        if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
                        staleOpenSet = true;
                    }
                }
            }
            if (review.openSet().isEmpty() || staleOpenSet) {
                next = nextPackage(userId, review.knowledgePointId(), review.lessonId(), revealed,
                        trap == null ? null : trap.code());
                if (next.isPresent()) {
                    try {
                        currentVersion = content.getPackageVersion(next.get().packageVersionId());
                    } catch (LearningRequestException error) {
                        if (error.getStatus() != 404 || !"NOT_FOUND".equals(error.getCode())) throw error;
                        next = Optional.empty();
                        currentVersion = null;
                    }
                }
            }
        }
        List<LearningContentClient.LessonPracticeSet> lessonPracticeSets = null;
        if (review.status() == ReviewStatus.PENDING && (review.stage() != ReviewStage.THEORY || archivedTheory)
                && next.isEmpty() && (review.openSet().isEmpty() || staleOpenSet)) {
            lessonPracticeSets = content.lessonPracticeSets(review.lessonId());
            if (lessonPracticeSets == null) lessonPracticeSets = List.of();
        }
        boolean openingForNoSets = review.status() == ReviewStatus.PENDING && review.blocking()
                && !"DIAGNOSTIC".equals(review.triggerKind()) && diagnosis.isEmpty()
                && (review.stage() != ReviewStage.THEORY || archivedTheory) && next.isEmpty()
                && (review.openSet().isEmpty() || staleOpenSet);
        DiagnosticFlow.Prepared diagnosticData = requiresDiagnosis(review, diagnosis) || openingForNoSets
                ? diagnosticPreparation.forSources(userId, List.of(review.knowledgePointId())) : null;
        Set<UUID> mistakesForLesson = lesson == null ? Set.of() : mistakes.of(userId, lesson);
        return new Prepared(lesson, focus, trap, diagnosticData, currentVersion, next, staleOpenSet,
                mistakesForLesson, lessonPracticeSets, review.stage(), review.status(), review.failedSets(),
                review.openSet().map(ReviewSet::id).orElse(null));
    }

    private ReadResult readUnderLock(UUID userId, UUID reviewId, Prepared prepared) {
        lock.lock(userId);
        ReviewItem review = ownedReview(userId, reviewId);
        if (review.status() != prepared.status() || review.stage() != prepared.stage()
                || review.failedSets() != prepared.failedSets()
                || !java.util.Objects.equals(review.openSet().map(ReviewSet::id).orElse(null), prepared.openSetId())) {
            return ReadResult.retryResult();
        }
        ErrorTrapResult errorTrap = prepared.errorTrap();
        Optional<DiagnosticSession> diagnosis = diagnostics.findByRootReview(reviewId);
        if (review.status() != ReviewStatus.PENDING) {
            return ReadResult.done(result(review, prepared.focus(), null, List.of(), diagnosis, List.of(), errorTrap));
        }
        if (diagnosis.isPresent() && diagnosis.get().probing()) {
            return ReadResult.done(result(review, prepared.focus(), null, List.of(), diagnosis, List.of(), errorTrap));
        }
        List<UUID> waitingOn = diagnostics.pendingWaitingOn(reviewId);
        if (!waitingOn.isEmpty()) {
            return ReadResult.done(result(review, prepared.focus(), null, List.of(), diagnosis, waitingOn, errorTrap));
        }
        if (requiresDiagnosis(review, diagnosis)) {
            if (prepared.diagnosticData() == null) return ReadResult.retryResult();
            diagnosis = diagnosticFlow.startAfterSecondFailure(review, prepared.diagnosticData());
            if (diagnosis.isPresent() && diagnosis.get().probing()) {
                return ReadResult.done(result(review, prepared.focus(), null, List.of(), diagnosis, List.of(), errorTrap));
            }
            if (diagnosis.isPresent() && diagnostics.findProbes(diagnosis.get().id()).isEmpty()) {
                diagnosis = Optional.empty();
            }
        }
        // No theory to read: the lesson is gone, or none of the learner's lessons teaches the point.
        boolean nothingToRead = prepared.lesson() == null || "NO_LESSON".equals(prepared.focus().scope());
        if (nothingToRead && review.stage() == ReviewStage.THEORY) {
            review.completeTheory();
            reviews.save(review);
        } else if (review.blocking() && review.isFresh() && !"PRACTICE".equals(review.triggerKind())
                && prepared.mistakesForLesson().contains(review.knowledgePointId())) {
            review.startWithTheory(TheoryReason.WRONG_IN_LESSON);
            reviews.save(review);
        }
        if (review.stage() == ReviewStage.THEORY) {
            return ReadResult.done(result(review, prepared.focus(), null, prepared.focus().quickCheck().stream()
                    .map(question -> new LessonResult.Question(question.questionVersionId(), question.sortOrder(),
                            question.stem(), options(question.options()), question.hint())).toList(), diagnosis,
                    List.of(), errorTrap));
        }
        Set<UUID> revealed = attempts.revealedPackageIds(userId);
        if (review.openSet().isPresent() && !prepared.staleOpenSet()
                && revealed.contains(review.openSet().orElseThrow().packageId())) {
            return ReadResult.retryResult();
        }
        if (review.openSet().isPresent() && (prepared.staleOpenSet()
                || revealed.contains(review.openSet().orElseThrow().packageId()))) {
            ReviewSet stale = review.discardOpenSet();
            reviews.deleteOpenSet(userId, reviewId, stale.id());
        }
        if (review.openSet().isEmpty()) {
            Optional<PracticeSet> next = prepared.next();
            if (next.isPresent() && (revealed.contains(next.get().packageId())
                    || reviews.assignedPackageIds(userId).contains(next.get().packageId()))) {
                return ReadResult.retryResult();
            }
            if (next.isEmpty()) {
                review.skip();
                reviews.save(review);
                if (prepared.diagnosticData() != null && diagnosis.isEmpty()) {
                    diagnosis = diagnosticFlow.startWhenOutOfSets(review, prepared.diagnosticData());
                }
                if (prepared.lessonPracticeSets() != null) {
                    practice.refreshPassForLesson(userId, review.lessonId(), prepared.lessonPracticeSets());
                }
                return ReadResult.done(result(review, prepared.focus(), null, List.of(), diagnosis, List.of(), errorTrap));
            }
            review.assignSet(UUID.randomUUID(), next.get().packageId(), next.get().packageVersionId());
            reviews.save(review);
        }
        ReviewSet set = review.openSet().orElseThrow();
        PackageVersion version = prepared.currentVersion();
        if (version == null || !set.packageVersionId().equals(version.packageVersionId())) return ReadResult.retryResult();
        Section first = ItemGrading.orderedSections(version).stream().findFirst().orElse(null);
        List<LessonResult.Question> questions = ItemGrading.orderedItems(version).stream()
                .map(item -> new LessonResult.Question(item.questionVersionId(), item.sortOrder(), item.stem(),
                        options(item.options()), item.hint())).toList();
        ReviewResult.Audio audio = first == null || first.audio() == null ? null
                : new ReviewResult.Audio(first.audio().mediaUrl(), first.audio().durationSeconds());
        return ReadResult.done(result(review, prepared.focus(), new ReviewResult.ReviewSet(set.id(), set.packageId(),
                set.packageVersionId(), first == null ? null : first.passage(), audio, questions), List.of(), diagnosis,
                List.of(), errorTrap));
    }

    private ReviewResult result(ReviewItem review, TheoryFocus.Focus focus, ReviewResult.ReviewSet set,
                                       List<LessonResult.Question> quickCheck, Optional<DiagnosticSession> diagnosis,
                                       List<UUID> waitingOn, ErrorTrapResult errorTrap) {
        boolean probing = review.status() == ReviewStatus.PENDING && diagnosis.isPresent() && diagnosis.get().probing();
        String state = probing ? ReviewResult.DIAGNOSING
                : !waitingOn.isEmpty() ? ReviewResult.WAITING_ON_PREREQUISITES : ReviewResult.ACTIVE;
        UUID sessionId = diagnosis.filter(session -> !diagnosticsPlaceholder(session)).map(DiagnosticSession::id)
                .orElse(null);
        return new ReviewResult(review.id(), review.status().name(), review.lessonId(), focus.theory(), set,
                review.knowledgePointId(), review.skill(), review.stage().name(),
                review.theoryReason() == null ? null : review.theoryReason().name(), focus.scope(),
                review.failedSets(), review.blocking() ? reviewSettings.unblockAfterFailedSets() : null,
                quickCheck, state, sessionId,
                review.blocking(), waitingOn, errorTrap, review.triggerKind());
    }

    private boolean diagnosticsPlaceholder(DiagnosticSession session) {
        return session.status() == DiagnosticSession.Status.CONCLUDED && diagnostics.findProbes(session.id()).isEmpty();
    }

    private static boolean requiresDiagnosis(ReviewItem review, Optional<DiagnosticSession> diagnosis) {
        return review.status() == ReviewStatus.PENDING && review.blocking() && review.failedSets() >= 2
                && !"DIAGNOSTIC".equals(review.triggerKind()) && diagnosis.isEmpty();
    }

    private record Prepared(Lesson lesson, TheoryFocus.Focus focus, ErrorTrapResult errorTrap,
                            DiagnosticFlow.Prepared diagnosticData,
                            PackageVersion currentVersion, Optional<PracticeSet> next, boolean staleOpenSet,
                            Set<UUID> mistakesForLesson, List<LearningContentClient.LessonPracticeSet> lessonPracticeSets,
                            ReviewStage stage, ReviewStatus status, int failedSets, UUID openSetId) {}

    private record ReadResult(ReviewResult result, boolean retry) {
        static ReadResult done(ReviewResult result) { return new ReadResult(result, false); }
        static ReadResult retryResult() { return new ReadResult(null, true); }
    }

    private static List<LessonResult.Option> options(List<LearningContentClient.Option> options) {
        return options == null ? null : options.stream()
                .map(option -> new LessonResult.Option(option.optionKey(), option.content(), option.sortOrder()))
                .toList();
    }

    private ReviewItem ownedReview(UUID userId, UUID reviewId) {
        return reviews.findOwned(userId, reviewId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Review was not found"));
    }

    /** Only a package whose answers the learner has not seen and that no earlier set gave can be assigned. */
    private Optional<PracticeSet> nextPackage(UUID userId, UUID knowledgePointId, UUID lessonId, Set<UUID> revealed,
                                              String trapErrorCode) {
        Set<UUID> excluded = new HashSet<>(revealed);
        excluded.addAll(reviews.assignedPackageIds(userId));
        List<PracticeSet> candidates = trapErrorCode == null
                ? content.searchPracticeSets(knowledgePointId, excluded.stream().toList(), MIN_SET_QUESTIONS, 1,
                        lessonId)
                : content.searchPracticeSets(knowledgePointId, excluded.stream().toList(), MIN_SET_QUESTIONS, 1,
                        lessonId, trapErrorCode);
        return candidates
                .stream().findFirst();
    }

    private ErrorTrapResult errorTrap(UUID userId, UUID parentKp) {
        if (errorTraps == null || profiles == null || errorTypeTexts == null) return null;
        var traps = errorTraps.load();
        var code = MainErrorRule.mainErrorCode(parentKp, profiles.findErrorTrapProfiles(userId), traps).orElse(null);
        if (code == null) return null;
        ErrorTypeTexts.Text text = errorTypeTexts.byCode().get(code);
        return text == null ? null : new ErrorTrapResult(code, text.name(), text.fixHint());
    }
}
