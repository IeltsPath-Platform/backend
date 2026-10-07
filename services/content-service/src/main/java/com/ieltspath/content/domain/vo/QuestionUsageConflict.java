package com.ieltspath.content.domain.vo;

import java.util.UUID;

/** Another owner of a question, regardless of which question version it uses. */
public record QuestionUsageConflict(UUID questionId, OwnerType ownerType, String ownerCode) {
    public enum OwnerType { LESSON, PACKAGE }
}
