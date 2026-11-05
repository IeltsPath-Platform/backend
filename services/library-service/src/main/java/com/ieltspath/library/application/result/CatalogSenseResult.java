package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.IeltsBandRange;
import com.ieltspath.library.domain.vo.PartOfSpeech;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One meaning of a dictionary word with its usage data; {@code bandRange} is derived from the CEFR level. */
public record CatalogSenseResult(
        UUID id,
        UUID vocabularyItemId,
        PartOfSpeech partOfSpeech,
        String englishDefinition,
        String vietnameseMeaning,
        String exampleSentence,
        String imageUrl,
        int sortOrder,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        CefrLevel cefrLevel,
        UUID topicId,
        String register,
        String grammarNote,
        String usageNote,
        String exampleTranslation,
        VocabularyTopicResult topic,
        IeltsBandRange bandRange,
        List<CollocationResult> collocations,
        List<WordRelationResult> synonyms,
        List<WordRelationResult> antonyms,
        List<CommonMistakeResult> commonMistakes,
        List<UsageExampleResult> examples,
        CatalogCountsResult counts
) {
}
