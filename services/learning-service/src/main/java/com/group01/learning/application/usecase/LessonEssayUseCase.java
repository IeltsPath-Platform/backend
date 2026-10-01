package com.group01.learning.application.usecase;

import com.group01.learning.application.LessonAccess;
import com.group01.learning.application.LessonEvidenceReference;
import com.group01.learning.application.exception.AccessUnavailableException;
import com.group01.learning.application.exception.InsufficientPointsException;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.exception.WritingGradingException;
import com.group01.learning.application.port.AccessClient;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.port.LearningProgressStore.NewEvidence;
import com.group01.learning.application.port.WritingSubmissionStore;
import com.group01.learning.application.port.WritingSubmissionStore.NewSubmission;
import com.group01.learning.application.port.WritingSubmissionStore.Submission;
import com.group01.learning.application.result.WritingSubmissionResult;
import com.group01.learning.application.writing.EssayGrader;
import com.group01.learning.application.writing.EssayPrompt;
import com.group01.learning.application.writing.WritingGrade;
import com.group01.learning.application.writing.WritingSettings;
import com.group01.learning.domain.service.WritingScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Grades a lesson essay within the request. Order: validate → look up {@code requestId} → load the lesson → check the
 * balance → [transaction: gate, record GRADING] → daily limit → LLM (no transaction, no lock) → store the grade as
 * PAYMENT_PENDING → debit (idempotent) → [transaction: GRADED + evidence]. A resend with the same {@code requestId}
 * continues from where the last one stopped, so the LLM runs and points are charged at most once per success.
 */
@Service
public class LessonEssayUseCase {
    static final String USAGE_KIND = "writing_grading";
    private static final Logger log = LoggerFactory.getLogger(LessonEssayUseCase.class);

    private final LearningContentClient content;
    private final LessonAccess access;
    private final LearningProgressStore progress;
    private final WritingSubmissionStore essays;
    private final EssayGrader grader;
    private final AccessClient points;
    private final WritingSettings settings;
    private final TransactionTemplate transaction;
    private final Clock clock = Clock.systemUTC();

    public LessonEssayUseCase(LearningContentClient content, LessonAccess access, LearningProgressStore progress,
                              WritingSubmissionStore essays, EssayGrader grader, AccessClient points,
                              WritingSettings settings, PlatformTransactionManager transactionManager) {
        this.content = content;
        this.access = access;
        this.progress = progress;
        this.essays = essays;
        this.grader = grader;
        this.points = points;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Only the owner reads a submission; another learner's id is not found. */
    public WritingSubmissionResult get(UUID userId, UUID submissionId) {
        return essays.findOwned(submissionId, userId).map(this::view)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Submission was not found"));
    }

    public WritingSubmissionResult submit(UUID userId, UUID lessonId, UUID blockId, UUID requestId, String essayText) {
        int words = validate(essayText);

        Optional<Submission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) {
            Submission submission = existing.get();
            if (!submission.userId().equals(userId) || !submission.lessonId().equals(lessonId)
                    || !submission.blockId().equals(blockId)) throw requestConflict();
            if ("GRADED".equals(submission.status())) return view(submission);
            if ("PAYMENT_PENDING".equals(submission.status())) return charge(submission);
        }
        if (!grader.available()) throw gradingUnavailable(null);

        Lesson lesson = content.getLesson(lessonId);
        Block block = lesson.blocks().stream()
                .filter(item -> item.blockId().equals(blockId) && "EXERCISE".equals(item.blockType()))
                .findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Essay block was not found"));
        if (!block.isEssay()) {
            throw new LearningRequestException(409, "NOT_ESSAY_BLOCK", "This block is not an essay");
        }
        requireBalance();

        Submission grading = transaction.execute(status -> startGrading(userId, lesson, block, requestId,
                essayText, words));
        if (!essays.incrementDailyUsage(userId, LocalDate.now(clock.withZone(ZoneId.of(settings.quotaTimezone()))),
                USAGE_KIND, settings.dailyGradingLimit())) {
            essays.markFailed(grading.id(), "DAILY_LIMIT_REACHED");
            throw new LearningRequestException(429, "DAILY_LIMIT_REACHED", "Daily grading limit reached");
        }

        WritingGrade grade;
        try {
            grade = grader.grade(grading.prompt(), grading.essayText());
        } catch (WritingGradingException exception) {
            essays.markFailed(grading.id(), exception.getCode());
            log.warn("Essay grading failed: submissionId={}, code={}", grading.id(), exception.getCode());
            throw gradingUnavailable(grading.id());
        }
        boolean passed = grading.prompt().passBand() != null
                && grade.overallBand().compareTo(grading.prompt().passBand()) >= 0;
        if (!essays.saveGrade(grading.id(), grade, passed)) throw gradingInProgress();
        return charge(essays.findByRequestId(requestId).orElseThrow());
    }

    /** Debit, then record GRADED and mastery evidence; resending PAYMENT_PENDING starts here. */
    private WritingSubmissionResult charge(Submission submission) {
        UUID ledgerEntryId;
        try {
            ledgerEntryId = points.debit(submission.userId(), submission.pointCost(), submission.id(),
                    "lesson-writing:" + submission.userId() + ":" + submission.requestId(), "Lesson essay grading");
        } catch (InsufficientPointsException exception) {
            essays.recordPaymentFailure(submission.id(), "INSUFFICIENT_POINTS");
            throw new LearningRequestException(402, "INSUFFICIENT_POINTS", "Not enough points to release the grade",
                    submission.id());
        } catch (AccessUnavailableException exception) {
            essays.recordPaymentFailure(submission.id(), "PAYMENT_UNAVAILABLE");
            throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable; resend later",
                    submission.id());
        }
        return transaction.execute(status -> finish(submission, ledgerEntryId));
    }

    private Submission startGrading(UUID userId, Lesson lesson, Block block, UUID requestId, String essayText,
                                    int words) {
        progress.lockUser(userId);
        access.authorize(userId, lesson);
        access.refresh(userId, lesson);
        Instant now = clock.instant();
        // A grading that outlived its request (for example a crash) no longer blocks the block.
        essays.abandonStaleGrading(userId, block.blockId(), now.minusSeconds(settings.staleGradingSeconds()));
        if (essays.hasGrading(userId, block.blockId())) throw gradingInProgress();
        Optional<Submission> existing = essays.findByRequestId(requestId);
        if (existing.isEmpty()) {
            essays.insertGrading(new NewSubmission(UUID.randomUUID(), userId, lesson.lessonId(), block.blockId(),
                    requestId, essayText, words, prompt(block), settings.pointCost(), now));
        } else if (!essays.restartGrading(existing.get().id(), now)) {
            // Only a FAILED row is graded again (on the essay it was first sent with); anything else raced ahead.
            throw gradingInProgress();
        }
        return essays.findByRequestId(requestId).orElseThrow();
    }

    private WritingSubmissionResult finish(Submission charged, UUID ledgerEntryId) {
        progress.lockUser(charged.userId());
        Submission submission = essays.findForUpdate(charged.id()).orElseThrow();
        if ("GRADED".equals(submission.status())) return view(submission);
        // Every essay before the block is first passed is new writing, so each one is evidence; none after.
        if (!essays.blockPassed(submission.userId(), submission.blockId(), submission.id())) {
            List<UUID> kpIds = new ArrayList<>(new LinkedHashSet<>(submission.prompt().knowledgePointIds()));
            Set<UUID> known = access.knownKnowledgePoints(submission.userId(), kpIds);
            List<NewEvidence> evidence = new ArrayList<>();
            for (UUID kpId : kpIds) {
                if (known.contains(kpId)) {
                    evidence.add(new NewEvidence(kpId, Boolean.TRUE.equals(submission.passed()), "lesson_writing",
                            LessonEvidenceReference.forWriting(submission.id(), kpId)));
                } else {
                    log.warn("Unknown writing evidence KP: userId={}, submissionId={}, kpId={}",
                            submission.userId(), submission.id(), kpId);
                }
            }
            progress.appendEvidence(submission.userId(), evidence);
        }
        essays.markGraded(submission.id(), ledgerEntryId);
        return view(essays.findForUpdate(submission.id()).orElseThrow());
    }

    /** The learner's view; the grade is shown only once GRADED. */
    private WritingSubmissionResult view(Submission submission) {
        String task = submission.prompt().task();
        if ("GRADED".equals(submission.status())) {
            boolean showSample = Boolean.TRUE.equals(submission.passed())
                    || essays.blockPassed(submission.userId(), submission.blockId(), submission.id());
            return new WritingSubmissionResult(submission.id(), submission.status(), task, submission.wordCount(),
                    submission.overallBand(), submission.passed(), submission.grade(), submission.pointCost(),
                    showSample ? submission.prompt().sampleAnswer() : null, null, null);
        }
        if ("PAYMENT_PENDING".equals(submission.status())) {
            return new WritingSubmissionResult(submission.id(), submission.status(), task, null, null, null, null,
                    null, null, submission.failureCode(), null);
        }
        return new WritingSubmissionResult(submission.id(), submission.status(), task, null, null, null, null, null,
                null, null, submission.failureCode());
    }

    private int validate(String essayText) {
        if (essayText == null || essayText.isBlank()) {
            throw new LearningRequestException(422, "ESSAY_EMPTY", "The essay is empty");
        }
        if (essayText.length() > settings.maxChars()) throw essayTooLong();
        int words = WritingScore.countWords(essayText);
        if (words < settings.minWords()) {
            throw new LearningRequestException(422, "ESSAY_TOO_SHORT",
                    "The essay needs at least " + settings.minWords() + " words");
        }
        if (words > settings.maxWords()) throw essayTooLong();
        return words;
    }

    private void requireBalance() {
        long balance;
        try {
            balance = points.balance();
        } catch (AccessUnavailableException exception) {
            throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable");
        }
        if (balance < settings.pointCost()) {
            throw new LearningRequestException(402, "INSUFFICIENT_POINTS",
                    "Grading costs " + settings.pointCost() + " points");
        }
    }

    /** Task 2 and Task 1 alike; chart facts and the sample answer stay server-side in the snapshot. */
    private static EssayPrompt prompt(Block block) {
        var question = block.questions().getFirst();
        Map<String, Object> spec = question.answerSpec() == null ? Map.of() : question.answerSpec();
        List<EssayPrompt.Image> images = question.assets() == null ? List.of() : question.assets().stream()
                .filter(asset -> "IMAGE".equals(asset.assetType()))
                .sorted(Comparator.comparingInt(LearningContentClient.QuestionAsset::sortOrder))
                .map(asset -> new EssayPrompt.Image(asset.mediaUrl(), asset.altText())).toList();
        return new EssayPrompt(question.questionVersionId(), question.stem(),
                spec.get("task") instanceof String task ? task : null,
                spec.get("minWords") instanceof Number words ? words.intValue() : null,
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null,
                spec.get("chartFacts") instanceof String facts ? facts : null,
                question.explanation(), images, List.copyOf(question.knowledgePointIds()));
    }

    private LearningRequestException essayTooLong() {
        return new LearningRequestException(422, "ESSAY_TOO_LONG", "The essay is too long");
    }

    private static LearningRequestException gradingUnavailable(UUID submissionId) {
        return new LearningRequestException(503, "GRADING_UNAVAILABLE", "Grading is unavailable; nothing was charged",
                submissionId);
    }

    private static LearningRequestException gradingInProgress() {
        return new LearningRequestException(409, "GRADING_IN_PROGRESS", "This essay block is already being graded");
    }

    private static LearningRequestException requestConflict() {
        return new LearningRequestException(409, "REQUEST_CONFLICT", "requestId belongs to another submission");
    }
}
