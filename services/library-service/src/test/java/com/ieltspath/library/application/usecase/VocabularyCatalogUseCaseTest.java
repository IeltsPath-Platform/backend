package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.application.result.VocabularyMindmapResult;
import com.ieltspath.library.domain.catalog.Collocation;
import com.ieltspath.library.domain.catalog.VocabularySenseDetails;
import com.ieltspath.library.domain.catalog.VocabularySetDraft;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VocabularyCatalogUseCaseTest {
    private final VocabularyCatalogStore store = mock(VocabularyCatalogStore.class);
    private final VocabularyCatalogUseCase useCase = new VocabularyCatalogUseCase(store);

    @Test
    void missingWordIsNotFound() {
        UUID id = UUID.randomUUID();
        when(store.findActiveItem(id)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.detail(id));
    }

    @Test
    void mindmapNeedsExactlyOneStartingPoint() {
        assertThrows(InvalidDataException.class, () -> useCase.mindmap(null, null));
        assertThrows(InvalidDataException.class, () -> useCase.mindmap(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void emptyMindmapIsNotFound() {
        UUID topic = UUID.randomUUID();
        when(store.topicMindmap(topic, 80)).thenReturn(new VocabularyMindmapResult(List.of(), List.of()));

        assertThrows(ResourceNotFoundException.class, () -> useCase.mindmap(topic, null));
    }

    @Test
    void senseUpdateRejectsAnUnknownTopicWithoutWriting() {
        UUID sense = UUID.randomUUID();
        UUID topic = UUID.randomUUID();
        when(store.topicExists(topic)).thenReturn(false);

        assertThrows(InvalidDataException.class,
                () -> useCase.updateSense(sense, new VocabularySenseDetails(null, topic, null, null, null, null)));
        verify(store, never()).updateSense(any(), any());
    }

    @Test
    void topicSetRejectsAnUnknownTopic() {
        UUID topic = UUID.randomUUID();
        when(store.topicExists(topic)).thenReturn(false);

        assertThrows(InvalidDataException.class, () -> useCase.createSet(
                new VocabularySetDraft("Environment", null, VocabularySetKind.TOPIC, null, topic, null, 0, null)));
        verify(store, never()).createSet(any());
    }

    @Test
    void childListIsLockedBeforeItIsReplaced() {
        UUID sense = UUID.randomUUID();
        when(store.lockSense(sense)).thenReturn(true);
        when(store.replaceCollocations(any(), any())).thenReturn(List.of());

        useCase.replaceCollocations(sense, List.of(new Collocation("make progress", null, null)));

        var order = org.mockito.Mockito.inOrder(store);
        order.verify(store).lockSense(sense);
        order.verify(store).replaceCollocations(any(), any());
    }

    @Test
    void childListOfMissingSenseIsNotFoundAndNotWritten() {
        UUID sense = UUID.randomUUID();
        when(store.lockSense(sense)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.replaceCollocations(sense, List.of(new Collocation("make progress", null, null))));
        verify(store, never()).replaceCollocations(any(), any());
    }

    @Test
    void oversizedChildListIsRejectedBeforeLocking() {
        List<Collocation> tooMany = IntStream.range(0, 31).mapToObj(i -> new Collocation("p" + i, null, null)).toList();

        assertThrows(InvalidDataException.class, () -> useCase.replaceCollocations(UUID.randomUUID(), tooMany));
        verify(store, never()).lockSense(any());
    }

    @Test
    void setMembershipMustAllExist() {
        UUID set = UUID.randomUUID();
        List<UUID> senses = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(store.lockSet(set)).thenReturn(true);
        when(store.countExistingSenses(senses)).thenReturn(1);

        assertThrows(ResourceNotFoundException.class, () -> useCase.replaceSetItems(set, senses));
        verify(store, never()).replaceSetItems(any(), any());
    }

    @Test
    void setMembershipIsReplacedWhenEverySenseExists() {
        UUID set = UUID.randomUUID();
        List<UUID> senses = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(store.lockSet(set)).thenReturn(true);
        when(store.countExistingSenses(senses)).thenReturn(2);

        useCase.replaceSetItems(set, senses);

        verify(store).replaceSetItems(set, senses);
        assertEquals(2, senses.size());
    }
}
