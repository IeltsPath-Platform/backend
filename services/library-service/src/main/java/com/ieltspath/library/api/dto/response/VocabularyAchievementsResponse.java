package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.VocabularyAchievementsResult;
import com.ieltspath.library.domain.progress.VocabularyProgressPolicy;

import java.util.List;

public record VocabularyAchievementsResponse(List<VocabularyProgressPolicy.Achievement> items) {
    public static VocabularyAchievementsResponse from(VocabularyAchievementsResult result) {
        return new VocabularyAchievementsResponse(result.items());
    }
}
