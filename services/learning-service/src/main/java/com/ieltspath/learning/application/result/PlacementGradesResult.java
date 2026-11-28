package com.ieltspath.learning.application.result;

import java.util.List;
import java.util.Map;

/**
 * The detailed grading of a placement test, as far as Learning graded it. {@code speaking} is null when no recording was
 * sent; its {@code result} has the shape of a Speaking practice result and is null unless graded. {@code writing} holds
 * the essays that were graded.
 */
public record PlacementGradesResult(Speaking speaking, List<PlacementEssayGrade> writing) {
    public record Speaking(String status, Map<String, Object> result) { }
}
