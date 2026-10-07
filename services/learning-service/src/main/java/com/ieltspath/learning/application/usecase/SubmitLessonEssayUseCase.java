package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Block;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.result.WritingSubmissionResult;
import com.ieltspath.learning.application.service.EssayPrompts;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.WritingSubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Grades a lesson essay within the request. Order: validate → look up {@code requestId} → load the lesson → check the
 * balance → [transaction: gate, record GRADING] → daily limit → LLM (no transaction, no lock) → store the grade as
 * PAYMENT_PENDING → debit (idempotent) → [transaction: GRADED + evidence]. A resend with the same {@code requestId}
 * continues from where the last one stopped, so the LLM runs and points are charged at most once per success.
 * {@link WritingSubmission} holds the status rules; writes outside the lock are compare-and-set on the status.
 */
@Service
public class SubmitLessonEssayUseCase {
    private static final Logger log = LoggerFactory.getLogger(SubmitLessonEssayUseCase.class);

    private final LearningContentClient content;
    private final LessonAccess access;
    private final LearnerLock lock;
    private final KnowledgeEvidenceRepository evidence;
    private final WritingSubmissionRepository essays;
    private final EssaySubmissionFlow flow;
    private final TransactionTemplate transaction;
    private final WritingSubmissionViewAssembler view;
    private final Clock clock = Clock.systemUTC();

    public SubmitLessonEssayUseCase(LearningContentClient content, LessonAccess access, LearnerLock lock,
                                    KnowledgeEvidenceRepository evidence, WritingSubmissionRepository essays,
                                    EssaySubmissionFlow flow, PlatformTransactionManager transactionManager,
                                    WritingSubmissionViewAssembler view) {
        this.content = content;
        this.access = access;
        this.lock = lock;
        this.evidence = evidence;
        this.essays = essays;
        this.flow = flow;
        this.transaction = new TransactionTemplate(transactionManager);
        this.view = view;
    }

    public WritingSubmissionResult execute(UUID userId, UUID lessonId, UUID blockId, UUID requestId, String essayText) {
        int words = flow.validate(essayText);

        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isPresent()) {
            WritingSubmission submission = existing.get();
            if (!submission.userId().equals(userId) || !lessonId.equals(submission.lessonId())
                    || !blockId.equals(submission.blockId())) throw EssaySubmissionFlow.requestConflict();
            if (submission.status() == WritingSubmissionStatus.GRADED) return view.assemble(submission);
            if (submission.status() == WritingSubmissionStatus.PAYMENT_PENDING) return charge(submission);
        }
        flow.requireGraderAvailable();

        Lesson lesson = content.getLesson(lessonId);
        Block block = lesson.blocks().stream()
                .filter(item -> item.blockId().equals(blockId) && "EXERCISE".equals(item.blockType()))
                .findFirst()
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Essay block was not found"));
        if (!block.isEssay()) {
            throw new LearningRequestException(409, "NOT_ESSAY_BLOCK", "This block is not an essay");
        }
        flow.requireBalance();

        WritingSubmission grading = transaction.execute(status -> startGrading(userId, lesson, block, requestId,
                essayText, words));
        flow.grade(grading);
        return charge(grading);
    }

    private WritingSubmissionResult charge(WritingSubmission submission) {
        return flow.charge(submission, "lesson-writing:" + submission.userId() + ":" + submission.requestId(),
                "Lesson essay grading", (charged, ledgerEntryId) -> transaction.execute(
                        status -> finish(charged, ledgerEntryId)));
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
            if (grading.isStale(now.minusSeconds(flow.staleGradingSeconds()))) {
                grading.abandon();
                if (!essays.save(grading)) gradingInProgress = true;
            } else {
                gradingInProgress = true;
            }
        }
        if (gradingInProgress) throw EssaySubmissionFlow.gradingInProgress();
        Optional<WritingSubmission> existing = essays.findByRequestId(requestId);
        if (existing.isEmpty()) {
            WritingSubmission submission = WritingSubmission.start(UUID.randomUUID(), userId, lesson.lessonId(),
                    block.blockId(), requestId, essayText, words, EssayPrompts.of(block.questions().getFirst()),
                    flow.pointCost(), now);
            essays.save(submission);
            return submission;
        }
        // Only a FAILED submission is graded again (on the essay it was first sent with); anything else raced ahead.
        WritingSubmission submission = existing.get();
        if (submission.status() != WritingSubmissionStatus.FAILED) throw EssaySubmissionFlow.gradingInProgress();
        submission.restart(now);
        if (!essays.save(submission)) throw EssaySubmissionFlow.gradingInProgress();
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
}
