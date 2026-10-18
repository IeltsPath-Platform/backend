package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;

public record PracticeTestCardResult(UUID packageId, String packageCode, UUID packageVersionId, String scope,
                                     String title, String skill, String coverReference, String sourceCode,
                                     String sourceName, List<String> formats, int questionCount,
                                     Integer timeLimitSeconds, String requiredFeatureKey, List<Part> parts) {
    public record Part(UUID sectionId, Integer partNo, String title, int questionCount, List<String> formats) {}
}
