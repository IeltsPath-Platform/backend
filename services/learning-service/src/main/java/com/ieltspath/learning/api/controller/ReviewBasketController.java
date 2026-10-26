package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.ReviewResponse;
import com.ieltspath.learning.api.dto.response.ReviewBasketResponse;
import com.ieltspath.learning.api.dto.response.ReviewBasketSummaryResponse;
import com.ieltspath.learning.application.usecase.GetReviewBasketSummaryUseCase;
import com.ieltspath.learning.application.usecase.GetReviewBasketUseCase;
import com.ieltspath.learning.application.usecase.StartBasketReviewUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/learning/review-basket")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReviewBasketController {
    private final CurrentUserProvider currentUser;
    private final GetReviewBasketUseCase basket;
    private final GetReviewBasketSummaryUseCase summary;
    private final StartBasketReviewUseCase start;

    public ReviewBasketController(CurrentUserProvider currentUser, GetReviewBasketUseCase basket,
                                  GetReviewBasketSummaryUseCase summary, StartBasketReviewUseCase start) {
        this.currentUser = currentUser;
        this.basket = basket;
        this.summary = summary;
        this.start = start;
    }

    @GetMapping
    public ReviewBasketResponse basket() {
        return ReviewBasketResponse.from(basket.execute(currentUser.requireUserId()));
    }

    /** The counts of the basket without its items, for a badge that is read on every page. */
    @GetMapping("/summary")
    public ReviewBasketSummaryResponse summary() {
        return ReviewBasketSummaryResponse.from(summary.execute(currentUser.requireUserId()));
    }

    /** Starts (or returns) an optional review of the point and gives its first set. */
    @PostMapping("/{knowledgePointId}/start")
    public ReviewResponse start(@PathVariable("knowledgePointId") UUID knowledgePointId) {
        return ReviewResponse.from(start.execute(currentUser.requireUserId(), knowledgePointId));
    }
}
