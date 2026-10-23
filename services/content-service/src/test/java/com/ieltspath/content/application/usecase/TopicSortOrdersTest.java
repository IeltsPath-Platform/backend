package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.TopicSortOrderTakenException;
import com.ieltspath.content.domain.repository.TopicRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicSortOrdersTest {

    private final TopicRepository topics = mock(TopicRepository.class);
    private final UUID parent = UUID.randomUUID();

    @Test
    void siblingWithTheSameOrderRefusesTheNewTopic() {
        when(topics.findByParentTopicId(parent)).thenReturn(List.of(topic(parent, 3)));

        assertThrows(TopicSortOrderTakenException.class,
                () -> TopicSortOrders.requireFree(topics, parent, 3, null));
    }

    @Test
    void sameOrderUnderAnotherParentIsAllowed() {
        when(topics.findByParentTopicId(parent)).thenReturn(List.of());
        when(topics.findByParentTopicId(UUID.randomUUID())).thenReturn(List.of(topic(UUID.randomUUID(), 3)));

        assertDoesNotThrow(() -> TopicSortOrders.requireFree(topics, parent, 3, null));
    }

    @Test
    void topicMayKeepItsOwnOrderWhenItIsTheOnlyHolder() {
        var self = topic(parent, 3);
        when(topics.findByParentTopicId(parent)).thenReturn(List.of(self));

        assertDoesNotThrow(() -> TopicSortOrders.requireFree(topics, parent, 3, self.getId()));
    }

    @Test
    void rootTopicsShareOneGroup() {
        when(topics.findRootTopics()).thenReturn(List.of(topic(UUID.randomUUID(), 1)));

        assertThrows(TopicSortOrderTakenException.class, () -> TopicSortOrders.requireFree(topics, null, 1, null));
    }

    private static com.ieltspath.content.domain.aggregate.Topic topic(UUID parentId, int sortOrder) {
        return com.ieltspath.content.domain.aggregate.Topic.create(parentId, "T-" + UUID.randomUUID(), "Topic", sortOrder, null, null);
    }
}
