package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.TopicSortOrderTakenException;
import com.ieltspath.content.domain.repository.TopicRepository;

import java.util.List;
import java.util.UUID;

/**
 * Sort order is a position among siblings, so it must be unique within one parent (roots share one group). A clash is
 * refused rather than resolved by moving another author's topic.
 */
final class TopicSortOrders {

    private TopicSortOrders() {
    }

    static void requireFree(TopicRepository topics, UUID parentTopicId, int sortOrder, UUID exceptTopicId) {
        List<Topic> siblings = parentTopicId == null ? topics.findRootTopics() : topics.findByParentTopicId(parentTopicId);
        boolean taken = siblings.stream()
                .anyMatch(sibling -> sibling.getSortOrder() == sortOrder && !sibling.getId().equals(exceptTopicId));
        if (taken) {
            throw new TopicSortOrderTakenException(sortOrder);
        }
    }
}
