package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.api.dto.response.ReviewResponse;
import com.ieltspath.learning.application.result.ReviewBasketResult;
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
    private final StartBasketReviewUseCase start;

    public ReviewBasketController(CurrentUserProvider currentUser, GetReviewBasketUseCase basket,
                                  StartBasketReviewUseCase start) {
        this.currentUser = currentUser;
        this.basket = basket;
        this.start = start;
    }

    @GetMapping
    public ReviewBasketResult basket() {
        return basket.execute(currentUser.requireUserId());
    }

    /** Starts (or returns) an optional review of the point and gives its first set. */
    @PostMapping("/{knowledgePointId}/start")
    public ReviewResponse start(@PathVariable("knowledgePointId") UUID knowledgePointId) {
        return ReviewResponse.from(start.execute(currentUser.requireUserId(), knowledgePointId));
    }
}
