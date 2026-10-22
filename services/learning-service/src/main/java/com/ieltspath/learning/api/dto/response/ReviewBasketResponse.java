package com.ieltspath.learning.api.dto.response;

import com.ieltspath.learning.application.result.ReviewBasketResult;

import java.util.List;

/** Additive basket groups keep the existing item fields and include pending optional weak points. */
public record ReviewBasketResponse(List<ReviewBasketResult.RequiredItem> required,
                                   List<ReviewBasketResult.OptionalItem> consolidate,
                                   List<ReviewBasketResult.OptionalItem> due,
                                   List<ReviewBasketResult.OptionalItem> weak) {
    public static ReviewBasketResponse from(ReviewBasketResult result) {
        return new ReviewBasketResponse(result.required(), result.consolidate(), result.due(), result.weak());
    }
}
