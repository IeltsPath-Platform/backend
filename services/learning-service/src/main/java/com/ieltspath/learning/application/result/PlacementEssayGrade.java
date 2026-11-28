package com.ieltspath.learning.application.result;

import com.ieltspath.learning.domain.vo.WritingGrade;

import java.math.BigDecimal;
import java.util.UUID;

/** One placement essay as stored after grading; {@code grade} is null when {@code failureCode} is set. */
public record PlacementEssayGrade(UUID attemptItemId, String task, int wordCount, Boolean passed, String essayText,
                                  WritingGrade grade, String failureCode) {
}
