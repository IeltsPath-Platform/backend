package com.group01.content.application.usecase;

import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.entity.VocabularySense;
import com.group01.content.domain.repository.VocabularyRepository;
import com.group01.content.domain.vo.PartOfSpeech;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchVocabularyUseCaseTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @InjectMocks
    private SearchVocabularyUseCase searchVocabularyUseCase;

    @Test
    @DisplayName("Should search vocabulary by query and map results with senses")
    void shouldSearchVocabularyByLemma() {
        VocabularyItem item = VocabularyItem.create("diligent", "/ˈdɪlɪdʒənt/", null);
        VocabularySense sense = VocabularySense.create(
                item.getId(),
                PartOfSpeech.ADJECTIVE,
                "showing care and effort in work",
                "chăm chỉ, cần cù",
                "She is a diligent student.",
                null,
                1
        );
        item.addSense(sense);

        when(vocabularyRepository.searchByLemma("diligent")).thenReturn(List.of(item));

        List<VocabularyItemResult> results = searchVocabularyUseCase.execute("  diligent  ");

        assertThat(results).hasSize(1);
        VocabularyItemResult first = results.get(0);
        assertThat(first.lemma()).isEqualTo("diligent");
        assertThat(first.senses()).hasSize(1);
        assertThat(first.senses().get(0).englishDefinition()).isEqualTo("showing care and effort in work");

        verify(vocabularyRepository).searchByLemma("diligent");
    }
}

