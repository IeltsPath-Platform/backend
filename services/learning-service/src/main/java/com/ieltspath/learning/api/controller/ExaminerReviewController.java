package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.port.HumanReviewClient;
import com.ieltspath.learning.application.usecase.HumanReviewUseCase;
import com.ieltspath.learning.application.usecase.ListExaminerReviewsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The learner's essays sent to an examiner: the list the notification "your essay was graded" leads to. Each item opens at
 * {@code /api/learning/writing-submissions/{submissionId}/human-review}, which holds the comments and the marked errors.
 */
@RestController
@RequestMapping("/api/learning/examiner-reviews")
@PreAuthorize("hasRole('CUSTOMER')")
public class ExaminerReviewController {
    private static final int STEM_PREVIEW = 160;

    private final CurrentUserProvider currentUser;
    private final ListExaminerReviewsUseCase reviews;
    private final HumanReviewUseCase humanReviews;

    public ExaminerReviewController(CurrentUserProvider currentUser, ListExaminerReviewsUseCase reviews,
                                    HumanReviewUseCase humanReviews) {
        this.currentUser = currentUser;
        this.reviews = reviews;
        this.humanReviews = humanReviews;
    }

    /**
     * The learner gives up a late request whose examiner can no longer be changed, and gets the credit back. Repeating it
     * after a failure finishes what was left; it answers the review as it stands ({@code CANCELLED}).
     */
    @PostMapping("/{reviewSubmissionId}/cancel")
    public HumanReviewClient.Review cancel(@PathVariable("reviewSubmissionId") UUID reviewSubmissionId) {
        return humanReviews.cancel(currentUser.requireUserId(), reviewSubmissionId);
    }

    @GetMapping
    public Response list(@RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size) {
        var result = reviews.execute(currentUser.requireUserId(), page, size);
        List<Item> items = result.items().stream().map(row -> new Item(row.submissionId(), row.reviewSubmissionId(),
                row.source(), row.task(), preview(row.stem()), row.wordCount(), row.requestedAt())).toList();
        return new Response(items, result.page(), result.size(), result.hasNext());
    }

    private static String preview(String stem) {
        if (stem == null) return null;
        String text = stem.strip();
        return text.length() <= STEM_PREVIEW ? text : text.substring(0, STEM_PREVIEW).stripTrailing() + "…";
    }

    /** {@code source} is {@code LESSON_BLOCK}, {@code PRACTICE_ITEM} or {@code FREE_WRITING}. */
    public record Item(UUID submissionId, UUID reviewSubmissionId, String source, String task, String promptPreview,
                       int wordCount, Instant requestedAt) {}

    public record Response(List<Item> items, int page, int size, boolean hasNext) {}
}
