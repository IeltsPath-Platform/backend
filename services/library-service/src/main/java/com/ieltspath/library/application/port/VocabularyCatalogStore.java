package com.ieltspath.library.application.port;

import com.ieltspath.library.application.query.CatalogBrowseQuery;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.CatalogItemResult;
import com.ieltspath.library.application.result.CollocationResult;
import com.ieltspath.library.application.result.CommonMistakeResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.result.UsageExampleResult;
import com.ieltspath.library.application.result.VocabularyMindmapResult;
import com.ieltspath.library.application.result.VocabularySetResult;
import com.ieltspath.library.application.result.VocabularySuggestionResult;
import com.ieltspath.library.application.result.VocabularyTopicResult;
import com.ieltspath.library.application.result.WordRelationResult;
import com.ieltspath.library.domain.catalog.Collocation;
import com.ieltspath.library.domain.catalog.CommonMistake;
import com.ieltspath.library.domain.catalog.UsageExample;
import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import com.ieltspath.library.domain.catalog.VocabularySenseDetails;
import com.ieltspath.library.domain.catalog.VocabularySetDraft;
import com.ieltspath.library.domain.catalog.VocabularyTopicDraft;
import com.ieltspath.library.domain.catalog.WordRelation;
import com.ieltspath.library.domain.vo.VocabularySetKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads and writes the dictionary catalog: words, their usage data, topics and sets. */
public interface VocabularyCatalogStore {
    Optional<CatalogItemResult> findActiveItem(UUID itemId);

    PageResult<CatalogItemResult> browse(CatalogBrowseQuery query);

    List<VocabularySuggestionResult> suggestions(String prefix);

    List<VocabularyTopicResult> activeTopics();

    PageResult<VocabularySetResult> activeSets(VocabularySetKind kind, PageQuery page);

    Optional<VocabularySetResult> findActiveSet(UUID setId);

    PageResult<CatalogItemResult> activeSetWords(UUID setId, UUID topicId, PageQuery page);

    VocabularyMindmapResult topicMindmap(UUID topicId, int maxNodes);

    VocabularyMindmapResult itemMindmap(UUID itemId, int maxNodes);

    boolean topicExists(UUID topicId);

    /** Returns the updated word, or empty when no word has this id. */
    Optional<CatalogItemResult> updateItem(UUID itemId, VocabularyItemDetails details);

    /** Returns the word that owns the updated sense, or empty when no sense has this id. */
    Optional<CatalogItemResult> updateSense(UUID senseId, VocabularySenseDetails details);

    /** Locks the sense so concurrent edits of its child lists are applied one after another. */
    boolean lockSense(UUID senseId);

    List<CollocationResult> replaceCollocations(UUID senseId, List<Collocation> values);

    List<WordRelationResult> replaceRelations(UUID senseId, List<WordRelation> values);

    List<CommonMistakeResult> replaceCommonMistakes(UUID senseId, List<CommonMistake> values);

    List<UsageExampleResult> replaceExamples(UUID senseId, List<UsageExample> values);

    List<VocabularyTopicResult> allTopics();

    /** Throws {@link com.ieltspath.library.domain.exception.ConflictException} when the slug is taken. */
    VocabularyTopicResult createTopic(VocabularyTopicDraft draft);

    /** Returns empty when no topic has this id. */
    Optional<VocabularyTopicResult> updateTopic(UUID topicId, VocabularyTopicDraft draft);

    boolean deactivateTopic(UUID topicId);

    List<VocabularySetResult> allSets();

    VocabularySetResult createSet(VocabularySetDraft draft);

    /** Returns empty when no set has this id. */
    Optional<VocabularySetResult> updateSet(UUID setId, VocabularySetDraft draft);

    boolean deactivateSet(UUID setId);

    /** Locks the set row; returns false when no set has this id. */
    boolean lockSet(UUID setId);

    int countExistingSenses(List<UUID> senseIds);

    void replaceSetItems(UUID setId, List<UUID> senseIds);
}
