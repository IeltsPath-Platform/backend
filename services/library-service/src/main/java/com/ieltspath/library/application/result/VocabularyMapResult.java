package com.ieltspath.library.application.result;

import java.util.List;

public record VocabularyMapResult(List<Band> bands, String currentBand, String suggestedBand) {
    public record Band(String band, String cefr, long total, long learned, long learning, long needsReview, long notLearned) {}
}
