package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.ReviewBasketSummaryResult;

/** The two counts a badge needs: blocking reviews, and maintained points due for review. */
public record ReviewBasketSummaryResponse(int required, int due) {
    public static ReviewBasketSummaryResponse from(ReviewBasketSummaryResult result) {
        return new ReviewBasketSummaryResponse(result.required(), result.due());
    }
}
