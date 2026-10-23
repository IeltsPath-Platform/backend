package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.TopicRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReorderTopicsUseCaseTest {

    private final TopicRepository topics = mock(TopicRepository.class);
    private final ReorderTopicsUseCase useCase = new ReorderTopicsUseCase(topics);

    private Topic topic(UUID parent, String code, int order) {
        return Topic.create(parent, code, code, order);
    }

    private List<UUID> ids(Topic... group) {
        List<UUID> ids = new ArrayList<>();
        for (Topic topic : group) ids.add(topic.getId());
        return ids;
    }

    @Test
    void theGroupKeepsItsOwnSortOrdersHandedOutInTheNewSequence() {
        Topic a = topic(null, "A", 910);
        Topic b = topic(null, "B", 920);
        Topic c = topic(null, "C", 960);
        when(topics.findRootTopics()).thenReturn(List.of(a, b, c));

        useCase.execute(null, ids(c, a, b));

        assertEquals(910, c.getSortOrder());
        assertEquals(920, a.getSortOrder());
        assertEquals(960, b.getSortOrder());
        verify(topics).save(a);
        verify(topics).save(b);
        verify(topics).save(c);
    }

    @Test
    void aTopicThatKeepsItsPositionIsNotWritten() {
        Topic a = topic(null, "A", 1);
        Topic b = topic(null, "B", 2);
        Topic c = topic(null, "C", 3);
        when(topics.findRootTopics()).thenReturn(List.of(a, b, c));

        useCase.execute(null, ids(a, c, b));

        verify(topics, never()).save(a);
        verify(topics).save(b);
        verify(topics).save(c);
    }

    @Test
    void aGroupWithTiedSortOrdersIsNumberedFromItsSmallestOrder() {
        Topic a = topic(null, "A", 5);
        Topic b = topic(null, "B", 5);
        Topic c = topic(null, "C", 9);
        when(topics.findRootTopics()).thenReturn(List.of(a, b, c));

        useCase.execute(null, ids(b, c, a));

        assertEquals(5, b.getSortOrder());
        assertEquals(6, c.getSortOrder());
        assertEquals(7, a.getSortOrder());
    }

    @Test
    void childrenOfAParentAreReorderedWithoutTouchingTheRoots() {
        Topic parent = topic(null, "P", 1);
        Topic x = topic(parent.getId(), "X", 1);
        Topic y = topic(parent.getId(), "Y", 2);
        when(topics.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(topics.findByParentTopicId(parent.getId())).thenReturn(List.of(x, y));

        useCase.execute(parent.getId(), ids(y, x));

        assertEquals(1, y.getSortOrder());
        assertEquals(2, x.getSortOrder());
        verify(topics, never()).findRootTopics();
    }

    @Test
    void theListMustNameEveryTopicOfTheParentOnceAndOnlyThose() {
        Topic a = topic(null, "A", 1);
        Topic b = topic(null, "B", 2);
        when(topics.findRootTopics()).thenReturn(List.of(a, b));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, ids(a)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, List.of(a.getId(), a.getId())));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, List.of(a.getId(), b.getId(), UUID.randomUUID())));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, List.of()));
        verify(topics, never()).save(any());
    }

    @Test
    void aTopicOfAnotherParentIsRefused() {
        Topic parent = topic(null, "P", 1);
        Topic child = topic(parent.getId(), "X", 1);
        Topic stranger = topic(null, "S", 2);
        when(topics.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(topics.findByParentTopicId(parent.getId())).thenReturn(List.of(child));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(parent.getId(), ids(stranger)));
        verify(topics, never()).save(any());
    }

    @Test
    void anUnknownParentIsNotFound() {
        UUID parent = UUID.randomUUID();
        when(topics.findById(parent)).thenReturn(Optional.empty());

        assertThrows(TopicNotFoundException.class, () -> useCase.execute(parent, List.of(UUID.randomUUID())));
    }
}
