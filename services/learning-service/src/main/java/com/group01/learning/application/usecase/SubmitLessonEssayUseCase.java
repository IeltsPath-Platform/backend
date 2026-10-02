package com.group01.learning.application.usecase;

import com.group01.learning.application.exception.AccessUnavailableException;
import com.group01.learning.application.exception.InsufficientPointsException;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.exception.WritingGradingException;
import com.group01.learning.application.port.AccessClient;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.LearningContentClient.Block;
import com.group01.learning.application.port.LearningContentClient.Lesson;
import com.group01.learning.application.port.LlmUsageQuota;
import com.group01.learning.application.result.WritingSubmissionResult;
import com.group01.learning.application.service.EssayGrader;
import com.group01.learning.application.service.LessonAccess;
import com.group01.learning.application.service.LessonEvidenceReference;
import com.group01.learning.application.service.WritingSettings;
import com.group01.learning.application.service.WritingSubmissionViewAssembler;
import com.group01.learning.domain.aggregate.WritingSubmission;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.repository.WritingSubmissionRepository;
import com.group01.learning.domain.service.WritingScore;
import com.group01.learning.domain.vo.EssayPrompt;
import com.group01.learning.domain.vo.EvidenceSource;
import com.group01.learning.domain.vo.KnowledgeEvidence;
import com.group01.learning.domain.vo.WritingGrade;
import com.group01.learning.domain.vo.WritingSubmissionStatus;
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
 * {@link WritingSubmission} holds the status rules; writes outside the lock are compare-and-set on the status.
 */
@Service
public class SubmitLessonEssayUseCase {
    static final String USAGE_KIND = "writing_grading";
    private static final Logger log = LoggerFactory.getLogger(SubmitLessonEssayUseCase.class);

    private final LearningContentClient content;
    private final LessonAccess access;
    private final LearnerLock lock;
    private final KnowledgeEvidenceRepository evidence;
    private final WritingSubmissionRepository essays;
    private final LlmUsageQuota quota;
    private final EssayGrader grader;
    private final AccessClient points;
    private final WritingSettings settings;
    private final TransactionTemplate transaction;
    private final WritingSubmissionViewAssembler view;
    private final Clock clock = Clock.systemUTC();

    public SubmitLessonEssayUseCase(LearningContentClient content, LessonAccess access, LearnerLock lock,
                                  KnowledgeEvidenceRepository evidence, WritingSubmissionRepository essays,
                                  LlmUsageQuota quota, EssayGrader grader, AccessClient points, WritingSettings settings,
                                  PlatformTransactionManager transactionManager, WritingSubmissionViewAssembler view) {
        this.content = content;
        this.access = access;
        this.lock = lock;
        this.evidence = evidence;
        this.essays = essays;
        this.quota = quota;
        this.grader = grader;
        this.points = points;
        this.settings = settings;
        this.transaction = new TransactionTemplate(transactionManager);
        this.view = view;
    }

    public WritingSubmissionResult execute(UUID userId, UUID lessonId, UUID blockId, UUID requestId, String essayText) {
        int words = validate(essayText);

        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) {
            WritingSubmission submission = existing.get();
            if (!submission.userId().equals(userId) || !submission.lessonId().equals(lessonId)
                    || !submission.blockId().equals(blockId)) throw requestConflict();
            if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
            if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) return charge(submission);
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

        WritingSubmission grading = transaction.execute(status -> startGrading(userId, lesson, block, requestId,
                essayText, words));
        if (!quota.tryConsume(userId, LocalDate.now(clock.withZone(ZoneId.of(settings.quotaTimezone()))),
                USAGE_KIND, settings.dailyGradingLimit())) {
            grading.fail("DAILY_LIMIT_REACHED");
            essays.save(grading);
            throw new LearningRequestException(429, "DAILY_LIMIT_REACHED", "Daily grading limit reached");
        }

        WritingGrade grade;
        try {
            grade = grader.grade(grading.prompt(), grading.essayText());
        } catch (WritingGradingException exception) {
            grading.fail(exception.getCode());
            essays.save(grading);
            log.warn("Essay grading failed: submissionId={}, code={}", grading.id(), exception.getCode());
            throw gradingUnavailable(grading.id());
        }
        grading.recordGrade(grade);
        // False when the grading was abandoned and taken over by another request meanwhile.
        if (!essays.save(grading)) throw gradingInProgress();
        return charge(grading);
    }

    /** Debit, then record GRADED and mastery evidence; resending PAYMENT_PENDING starts here. */
    private WritingSubmissionResult charge(WritingSubmission submission) {
        UUID ledgerEntryId;
        try {
            ledgerEntryId = points.debit(submission.userId(), submission.pointCost(), submission.id(),
                    "lesson-writing:" + submission.userId() + ":" + submission.requestId(), "Lesson essay grading");
        } catch (InsufficientPointsException exception) {
            submission.recordPaymentFailure("INSUFFICIENT_POINTS");
            essays.save(submission);
            throw new LearningRequestException(402, "INSUFFICIENT_POINTS", "Not enough points to release the grade",
                    submission.id());
        } catch (AccessUnavailableException exception) {
            submission.recordPaymentFailure("PAYMENT_UNAVAILABLE");
            essays.save(submission);
            throw new LearningRequestException(503, "PAYMENT_UNAVAILABLE", "Payment is unavailable; resend later",
                    submission.id());
        }
        return transaction.execute(status -> finish(submission, ledgerEntryId));
    }

    private WritingSubmission startGrading(UUID userId, Lesson lesson, Block block, UUID requestId, String essayText,
                                           int words) {
        lock.lock(userId);
        var context = access.authorize(userId, lesson);
        access.refresh(userId, lesson, context.progress());
        Instant now = clock.instant();
        boolean gradingInProgress = false;
        for (WritingSubmission grading : essays.findGrading(userId, block.blockId())) {
            // A grading that outlived its request (for example a crash) no longer blocks the block.
            if (grading.isStale(now.minusSeconds(settings.staleGradingSeconds()))) {
                grading.abandon();
                if (!essays.save(grading)) gradingInProgress = true;
            } else {
                gradingInProgress = true;
            }
        }
        if (gradingInProgress) throw gradingInProgress();
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isEmpty()) {
            WritingSubmission submission = WritingSubmission.start(UUID.randomUUID(), userId, lesson.lessonId(),
                    block.blockId(), requestId, essayText, words, prompt(block), settings.pointCost(), now);
            essays.save(submission);
            return submission;
        }
        // Only a FAILED submission is graded again (on the essay it was first sent with); anything else raced ahead.
        WritingSubmission submission = existing.get();
        if (submission.status() != WritingSubmissionStatus.FAILED) throw gradingInProgress();
        submission.restart(now);
        if (!essays.save(submission)) throw gradingInProgress();
        return submission;
    }

    /** Learner lock before the row lock, the same order as every other learner write. */
    private WritingSubmissionResult finish(WritingSubmission charged, UUID ledgerEntryId) {
        lock.lock(charged.userId());
        WritingSubmission submission = essays.findForUpdate(charged.id()).orElseThrow();
        if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
        // Every essay before the block is first passed is new writing, so each one is evidence; none after.
        if (!essays.blockPassed(submission.userId(), submission.blockId(), submission.id())) {
            List<UUID> kpIds = new ArrayList<>(new LinkedHashSet<>(submission.prompt().knowledgePointIds()));
            Set<UUID> known = access.knownKnowledgePoints(submission.userId(), kpIds);
            List<KnowledgeEvidence> essayEvidence = new ArrayList<>();
            for (UUID kpId : kpIds) {
                if (known.contains(kpId)) {
                    essayEvidence.add(KnowledgeEvidence.of(kpId, Boolean.TRUE.equals(submission.passed()),
                            EvidenceSource.LESSON_WRITING, LessonEvidenceReference.forWriting(submission.id(), kpId)));
                } else {
                    log.warn("Unknown writing evidence KP: userId={}, submissionId={}, kpId={}",
                            submission.userId(), submission.id(), kpId);
                }
            }
            evidence.append(submission.userId(), essayEvidence);
        }
        submission.markGraded(ledgerEntryId);
        essays.save(submission);
        return view.assemble(submission);
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
