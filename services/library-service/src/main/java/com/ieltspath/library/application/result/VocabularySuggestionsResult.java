package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.PartOfSpeech;
import java.util.List;
import java.util.UUID;

public record VocabularySuggestionsResult(List<Word> content, int page, int size, boolean hasMore) {
    public record Word(UUID id, String lemma, String ipa, List<Sense> senses) {}
    public record Sense(UUID id, PartOfSpeech partOfSpeech, String englishDefinition, String vietnameseMeaning) {}
}
