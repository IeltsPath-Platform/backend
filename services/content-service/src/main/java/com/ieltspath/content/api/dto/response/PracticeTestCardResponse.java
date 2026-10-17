package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.PracticeTestCardResult;
import java.util.List;
import java.util.UUID;

public record PracticeTestCardResponse(UUID packageId, String packageCode, UUID packageVersionId, String scope,
                                       String title, String skill, String coverUrl, Source source,
                                       List<String> formats, int questionCount, Integer timeLimitSeconds,
                                       String requiredFeatureKey, List<Part> parts) {
    public record Source(String code, String name) {}
    public record Part(UUID sectionId, Integer partNo, String title, int questionCount, List<String> formats) {}
    public static PracticeTestCardResponse from(PracticeTestCardResult card, String coverUrl) {
        return new PracticeTestCardResponse(card.packageId(), card.packageCode(), card.packageVersionId(), card.scope(),
                card.title(), card.skill(), coverUrl, card.sourceCode() == null ? null
                : new Source(card.sourceCode(), card.sourceName()), card.formats(), card.questionCount(),
                card.timeLimitSeconds(), card.requiredFeatureKey(), card.parts().stream()
                .map(part -> new Part(part.sectionId(), part.partNo(), part.title(), part.questionCount(), part.formats())).toList());
    }
}
