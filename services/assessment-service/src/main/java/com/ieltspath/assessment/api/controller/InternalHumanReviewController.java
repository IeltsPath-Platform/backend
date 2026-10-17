package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateHumanReviewRequest;
import com.ieltspath.assessment.api.dto.response.HumanReviewResponse;
import com.ieltspath.assessment.application.port.HumanReviewStore;
import com.ieltspath.assessment.application.service.HumanReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
public class InternalHumanReviewController {
    private final HumanReviewService reviews;

    public InternalHumanReviewController(HumanReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping("/internal/assessments/human-reviews")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    public HumanReviewResponse create(@Valid @RequestBody CreateHumanReviewRequest request) {
        HumanReviewStore.Review review = reviews.create(new HumanReviewStore.CreateReview(request.requestId(),
                request.learnerId(), request.sourceSubmissionId(), request.source().name(), request.task(),
                request.promptSnapshot(), request.essay(), request.rubric()));
        return HumanReviewResponse.created(review);
    }

    @GetMapping("/internal/assessments/human-reviews/{reviewSubmissionId}")
    public HumanReviewResponse get(@PathVariable("reviewSubmissionId") UUID reviewSubmissionId,
                                   @RequestParam("learnerId") UUID learnerId) {
        return HumanReviewResponse.result(reviews.get(reviewSubmissionId, learnerId));
    }
}
