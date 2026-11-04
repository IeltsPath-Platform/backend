package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.port.PronunciationAudioUrls;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchVocabularyUseCaseTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private PronunciationAudioUrls audioUrls;

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
        when(audioUrls.resolve(null)).thenReturn(null);

        List<VocabularyItemResult> results = searchVocabularyUseCase.execute("  diligent  ");

        assertThat(results).hasSize(1);
        VocabularyItemResult first = results.get(0);
        assertThat(first.lemma()).isEqualTo("diligent");
        assertThat(first.senses()).hasSize(1);
        assertThat(first.senses().get(0).englishDefinition()).isEqualTo("showing care and effort in work");

        verify(vocabularyRepository).searchByLemma("diligent");
    }

    @Test
    void returnsResolvedPreferredAudioReference() {
        Instant now = Instant.parse("2026-11-03T00:00:00Z");
        VocabularyItem item = new VocabularyItem(UUID.randomUUID(), "diligent", "diligent", "/d/",
                "legacy.mp3", "uk.mp3", "us.mp3", ContentStatus.ACTIVE, now, now, List.of());
        when(vocabularyRepository.searchByLemma("diligent")).thenReturn(List.of(item));
        when(audioUrls.resolve("us.mp3")).thenReturn("https://storage.example/media/audio.mp3");

        var result = searchVocabularyUseCase.execute("diligent");

        assertThat(result.getFirst().pronunciationAudioReference()).isEqualTo("https://storage.example/media/audio.mp3");
        org.mockito.Mockito.verify(audioUrls).resolve("us.mp3");
    }
}
