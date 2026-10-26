package com.ieltspath.content.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record AuthorSectionQuestionResult(UUID id, UUID questionVersionId, int sortOrder, BigDecimal maxScore) {
}
