package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.progress.VocabularyProgressPolicy;

import java.util.List;

public record VocabularyAchievementsResult(List<VocabularyProgressPolicy.Achievement> items) {}
