package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.usecase.GetPracticeSinceEssayUseCase;
import com.ieltspath.learning.application.usecase.ListSubmissionHistoryUseCase;
import com.ieltspath.learning.application.usecase.ListSubmissionHistoryUseCase.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The learner's own submissions for the page "Lịch sử nộp bài": inside a course ({@code scope=COURSE}), free practice
 * Writing and Speaking ({@code scope=FREE}, {@code skill=WRITING|SPEAKING}), or free practice on one knowledge point
 * ({@code scope=KNOWLEDGE}). Free practice Listening and Reading are listed by Assessment
 * ({@code GET /api/assessments/practice/history}). See {@code docs/contracts/submission-history-v1.md}.
 */
@RestController
@RequestMapping("/api/learning/submission-history")
@PreAuthorize("hasRole('CUSTOMER')")
public class SubmissionHistoryController {
    private static final int STEM_PREVIEW = 160;

    private final CurrentUserProvider currentUser;
    private final ListSubmissionHistoryUseCase history;
    private final GetPracticeSinceEssayUseCase practiceSinceEssay;

    public SubmissionHistoryController(CurrentUserProvider currentUser, ListSubmissionHistoryUseCase history,
                                       GetPracticeSinceEssayUseCase practiceSinceEssay) {
        this.currentUser = currentUser;
        this.history = history;
        this.practiceSinceEssay = practiceSinceEssay;
    }

    @GetMapping
    public Response list(@RequestParam("scope") Scope scope,
                         @RequestParam(name = "skill", required = false) String skill,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size) {
        var result = history.execute(currentUser.requireUserId(), scope, skill, page, size);
        List<Item> items = result.items().stream().map(entry -> new Item(entry.kind(), entry.id(), entry.submittedAt(),
                entry.skill(), entry.task(), preview(entry.stem()), entry.band(), entry.percent(), entry.correctCount(),
                entry.totalCount(), entry.passed(), entry.status(), entry.lessonId(), entry.topicId(), entry.courseId(),
                entry.attemptId(), entry.packageVersionId(), entry.revisionNo(), entry.knowledgePointId(),
                entry.knowledgePointCode(), entry.knowledgePointName())).toList();
        return new Response(items, result.page(), result.size(), result.hasNext());
    }

    /**
     * The free practice the learner did on {@code knowledgePointIds} since essay {@code submissionId} was submitted, one entry
     * per point practised; a point not practised since is absent. At most 20 points; another learner's essay is not found.
     */
    @GetMapping("/practice-since-essay")
    public List<PracticeSince> practiceSinceEssay(@RequestParam("submissionId") UUID submissionId,
                                                  @RequestParam(name = "knowledgePointIds", required = false) List<UUID> knowledgePointIds) {
        return practiceSinceEssay.execute(currentUser.requireUserId(), submissionId, knowledgePointIds).stream()
                .map(tally -> new PracticeSince(tally.knowledgePointId(), tally.attempts(), tally.lastAttemptId(),
                        tally.lastSubmittedAt(), tally.lastCorrectCount(), tally.lastTotalCount(), tally.lastPassed()))
                .toList();
    }

    private static String preview(String stem) {
        if (stem == null) return null;
        String text = stem.strip();
        return text.length() <= STEM_PREVIEW ? text : text.substring(0, STEM_PREVIEW).stripTrailing() + "…";
    }

    /**
     * {@code kind}: PRACTICE, WRITING, SPEAKING, TOPIC_TEST, COURSE_TEST, PRACTICE_TEST or KP_PRACTICE; fields that do not
     * apply are null. The knowledge point fields belong to KP_PRACTICE; its code and name are null when the catalog is down.
     */
    public record Item(String kind, UUID id, Instant submittedAt, String skill, String task, String promptPreview, BigDecimal band,
                       Double percent, Integer correctCount, Integer totalCount, Boolean passed, String status, UUID lessonId,
                       UUID topicId, UUID courseId, UUID attemptId, UUID packageVersionId, Integer revisionNo,
                       UUID knowledgePointId, String knowledgePointCode, String knowledgePointName) {}

    public record Response(List<Item> items, int page, int size, boolean hasNext) {}

    /** Free practice of one knowledge point since an essay: how many sets, and the latest one. */
    public record PracticeSince(UUID knowledgePointId, int attempts, UUID lastAttemptId, Instant lastSubmittedAt,
                                Integer lastCorrectCount, Integer lastTotalCount, Boolean lastPassed) {}
}
