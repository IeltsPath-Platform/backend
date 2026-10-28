package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindVocabularyByLemmasUseCaseTest {

    @Mock
    private VocabularyRepository vocabulary;

    @Test
    void returnsActiveWordsWithTheirActiveSensesInOrder() {
        VocabularyItem item = VocabularyItem.create("Trend", null, null);
        item.addSense(sense(item, 2, ContentStatus.ACTIVE));
        item.addSense(sense(item, 1, ContentStatus.ACTIVE));
        item.addSense(sense(item, 0, ContentStatus.INACTIVE));
        VocabularyItem withoutSenses = VocabularyItem.create("plummet", null, null);
        VocabularyItem inactive = new VocabularyItem(UUID.randomUUID(), "going", "going", null, null,
                ContentStatus.INACTIVE, Instant.now(), Instant.now(), new ArrayList<>());
        inactive.addSense(sense(inactive, 0, ContentStatus.ACTIVE));
        when(vocabulary.findByNormalizedLemmas(List.of("trend", "plummet", "going")))
                .thenReturn(List.of(item, withoutSenses, inactive));

        List<VocabularyItemResult> result = new FindVocabularyByLemmasUseCase(vocabulary)
                .execute(List.of(" Trend ", "plummet", "going", "TREND"));

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.lemma()).isEqualTo("Trend");
            assertThat(found.senses()).extracting(sense -> sense.sortOrder()).containsExactly(1, 2);
        });
    }

    private static VocabularySense sense(VocabularyItem item, int sortOrder, ContentStatus status) {
        return new VocabularySense(UUID.randomUUID(), item.getId(), PartOfSpeech.NOUN, "def", "nghĩa", "Example.",
                null, sortOrder, status, Instant.now(), Instant.now());
    }
}
