package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.ResolvedVocabularyResult;

import java.util.UUID;

public record ResolvedVocabularyResponse(String lemma, UUID senseId, boolean created) {

    public static ResolvedVocabularyResponse from(ResolvedVocabularyResult result) {
        return new ResolvedVocabularyResponse(result.lemma(), result.senseId(), result.created());
    }
}
