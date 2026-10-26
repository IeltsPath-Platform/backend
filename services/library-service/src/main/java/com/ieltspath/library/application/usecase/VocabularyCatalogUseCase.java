package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyCatalogStore;
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
import com.ieltspath.library.domain.catalog.CatalogLimits;
import com.ieltspath.library.domain.catalog.Collocation;
import com.ieltspath.library.domain.catalog.CommonMistake;
import com.ieltspath.library.domain.catalog.UsageExample;
import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import com.ieltspath.library.domain.catalog.VocabularySenseDetails;
import com.ieltspath.library.domain.catalog.VocabularySetDraft;
import com.ieltspath.library.domain.catalog.VocabularyTopicDraft;
import com.ieltspath.library.domain.catalog.WordRelation;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Public dictionary reads and the author edits of the catalog. */
@Service
@RequiredArgsConstructor
public class VocabularyCatalogUseCase {
    /** Keeps the mindmap readable and its query bounded. */
    static final int MINDMAP_MAX_NODES = 80;

    private final VocabularyCatalogStore store;

    @Transactional(readOnly = true)
    public CatalogItemResult detail(UUID itemId) {
        return store.findActiveItem(itemId).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public PageResult<CatalogItemResult> browse(CatalogBrowseQuery query) {
        return store.browse(query);
    }

    @Transactional(readOnly = true)
    public List<VocabularySuggestionResult> suggestions(String prefix) {
        return prefix == null || prefix.isBlank() ? List.of() : store.suggestions(prefix.trim());
    }

    @Transactional(readOnly = true)
    public List<VocabularyTopicResult> topics() {
        return store.activeTopics();
    }

    @Transactional(readOnly = true)
    public PageResult<VocabularySetResult> sets(VocabularySetKind kind, PageQuery page) {
        return store.activeSets(kind, page);
    }

    @Transactional(readOnly = true)
    public VocabularySetResult set(UUID setId) {
        return store.findActiveSet(setId).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public PageResult<CatalogItemResult> setWords(UUID setId, UUID topicId, PageQuery page) {
        if (store.findActiveSet(setId).isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return store.activeSetWords(setId, topicId, page);
    }

    @Transactional(readOnly = true)
    public VocabularyMindmapResult mindmap(UUID topicId, UUID itemId) {
        if ((topicId == null) == (itemId == null)) {
            throw new InvalidDataException("provide exactly one of topicId or itemId");
        }
        VocabularyMindmapResult map = topicId != null
                ? store.topicMindmap(topicId, MINDMAP_MAX_NODES)
                : store.itemMindmap(itemId, MINDMAP_MAX_NODES);
        if (map.nodes().isEmpty()) {
            throw new ResourceNotFoundException();
        }
        return map;
    }

    @Transactional
    public CatalogItemResult updateItem(UUID itemId, VocabularyItemDetails details) {
        return store.updateItem(itemId, details).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public CatalogItemResult updateSense(UUID senseId, VocabularySenseDetails details) {
        requireTopic(details.topicId());
        return store.updateSense(senseId, details).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public List<CollocationResult> replaceCollocations(UUID senseId, List<Collocation> values) {
        return replaceChildren(senseId, values, list -> store.replaceCollocations(senseId, list));
    }

    @Transactional
    public List<WordRelationResult> replaceRelations(UUID senseId, List<WordRelation> values) {
        return replaceChildren(senseId, values, list -> store.replaceRelations(senseId, list));
    }

    @Transactional
    public List<CommonMistakeResult> replaceCommonMistakes(UUID senseId, List<CommonMistake> values) {
        return replaceChildren(senseId, values, list -> store.replaceCommonMistakes(senseId, list));
    }

    @Transactional
    public List<UsageExampleResult> replaceExamples(UUID senseId, List<UsageExample> values) {
        return replaceChildren(senseId, values, list -> store.replaceExamples(senseId, list));
    }

    @Transactional(readOnly = true)
    public List<VocabularyTopicResult> allTopics() {
        return store.allTopics();
    }

    @Transactional
    public VocabularyTopicResult createTopic(VocabularyTopicDraft draft) {
        return store.createTopic(draft);
    }

    @Transactional
    public VocabularyTopicResult updateTopic(UUID topicId, VocabularyTopicDraft draft) {
        return store.updateTopic(topicId, draft).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public void deactivateTopic(UUID topicId) {
        if (!store.deactivateTopic(topicId)) {
            throw new ResourceNotFoundException();
        }
    }

    @Transactional(readOnly = true)
    public List<VocabularySetResult> allSets() {
        return store.allSets();
    }

    @Transactional
    public VocabularySetResult createSet(VocabularySetDraft draft) {
        requireTopic(draft.topicId());
        return store.createSet(draft);
    }

    @Transactional
    public VocabularySetResult updateSet(UUID setId, VocabularySetDraft draft) {
        requireTopic(draft.topicId());
        return store.updateSet(setId, draft).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public void deactivateSet(UUID setId) {
        if (!store.deactivateSet(setId)) {
            throw new ResourceNotFoundException();
        }
    }

    @Transactional
    public void replaceSetItems(UUID setId, List<UUID> senseIds) {
        List<UUID> members = CatalogLimits.requireSetSenses(senseIds);
        if (!store.lockSet(setId)) {
            throw new ResourceNotFoundException();
        }
        if (!members.isEmpty() && store.countExistingSenses(members) != members.size()) {
            throw new ResourceNotFoundException();
        }
        store.replaceSetItems(setId, members);
    }

    private <T, R> List<R> replaceChildren(UUID senseId, List<T> values, Function<List<T>, List<R>> replace) {
        List<T> entries = CatalogLimits.requireChildren(values);
        if (!store.lockSense(senseId)) {
            throw new ResourceNotFoundException();
        }
        return replace.apply(entries);
    }

    /** An unknown topic is the author's mistake, not a missing page, so it is reported as invalid input. */
    private void requireTopic(UUID topicId) {
        if (topicId != null && !store.topicExists(topicId)) {
            throw new InvalidDataException("topicId does not match a vocabulary topic");
        }
    }
}
