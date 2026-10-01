package com.group01.library.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.library.application.command.GetGameContentSnapshotCommand;
import com.group01.library.domain.aggregate.VocabularyItem;
import com.group01.library.domain.entity.VocabularySense;
import com.group01.library.domain.repository.VocabularyRepository;
import com.group01.library.domain.vo.PartOfSpeech;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetGameContentSnapshotUseCaseTest {
    private final VocabularyRepository repository = mock(VocabularyRepository.class);
    private final GetGameContentSnapshotUseCase useCase = new GetGameContentSnapshotUseCase(repository, new ObjectMapper());

    @Test
    void snapshotsActiveVocabularyWithExistingContract() {
        VocabularyItem item = VocabularyItem.create("apple", null, null);
        VocabularySense sense = VocabularySense.create(item.getId(), PartOfSpeech.NOUN,
                "a fruit", "quả táo", "An apple.", null, 0);
        item.addSense(sense);
        when(repository.findByIds(List.of(item.getId()))).thenReturn(List.of(item));

        var result = useCase.execute(new GetGameContentSnapshotCommand(
                "WORD_MEANING_MATCH", "VOCABULARY", List.of(item.getId())));
        assertEquals(1, result.items().size());
        assertEquals(sense.getId(), result.items().getFirst().vocabularySenseId());
        assertTrue(result.items().getFirst().answerSpecJson().contains("quả táo"));
    }

    @Test
    void refusesGrammarDomain() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                new GetGameContentSnapshotCommand("WORD_ORDER", "GRAMMAR", List.of(UUID.randomUUID()))));
        verifyNoInteractions(repository);
    }
}
