package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.application.result.VocabularyReviewResult;
import com.ieltspath.library.application.usecase.VocabularyReviewUseCase;
import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-support/flashcards")
public class VocabularyReviewController {
    private final CurrentUserProvider currentUser;
    private final VocabularyReviewUseCase reviews;
    public VocabularyReviewController(CurrentUserProvider currentUser,VocabularyReviewUseCase reviews) {
        this.currentUser=currentUser;this.reviews=reviews;
    }
    public record StudyRequest(@NotNull UUID vocabularySenseId,@NotNull Boolean active) {}
    public record SessionRequest(@NotNull UUID requestId,@Min(1) @Max(50) Integer limit) {}
    public record AnswerRequest(@NotNull UUID requestId,@NotNull UUID vocabularySenseId,
                                @NotNull @Min(0) Long expectedVersion,@NotNull VocabularyProgress.Outcome outcome) {}
    @PostMapping("/vocabulary-study-items")
    public VocabularyReviewResult.Progress study(@Valid @RequestBody StudyRequest r) {
        return reviews.study(currentUser.requireUserId(),r.vocabularySenseId(),r.active());
    }
    @GetMapping("/statistics/vocabulary")
    public VocabularyReviewResult.Statistics statistics() { return reviews.statistics(currentUser.requireUserId()); }
    @PostMapping("/vocabulary-review-sessions")
    public VocabularyReviewResult.Session start(@Valid @RequestBody SessionRequest r) {
        return reviews.start(currentUser.requireUserId(),r.requestId(),r.limit()==null?20:r.limit());
    }
    @PostMapping("/vocabulary-review-sessions/{id}/answers")
    public VocabularyReviewResult.Progress answer(@PathVariable("id") UUID id,@Valid @RequestBody AnswerRequest r) {
        return reviews.answer(currentUser.requireUserId(),id,r.requestId(),r.vocabularySenseId(),r.expectedVersion(),r.outcome());
    }
    @GetMapping("/statistics/study-context")
    public VocabularyReviewResult.StudyContext context(@RequestParam("sourceType") String sourceType,
            @RequestParam("sourceId") UUID sourceId) {
        return reviews.context(currentUser.requireUserId(),sourceType,sourceId);
    }
}
