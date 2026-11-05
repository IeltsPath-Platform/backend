package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.application.result.WordAudioResult;
import com.ieltspath.library.application.result.WordLookupResult;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookupVocabularyWordUseCaseTest {

    @Mock
    private VocabularyRepository vocabulary;
    @Mock
    private VocabularyCatalogStore catalog;

    private LookupVocabularyWordUseCase useCase() {
        return new LookupVocabularyWordUseCase(vocabulary, catalog);
    }

    @Test
    void findsAnInactiveWordWithItsPronunciationAndNoSense() {
        VocabularyItem pronunciationOnly = new VocabularyItem(UUID.randomUUID(), "live", "live", "/laɪv/", null,
                ContentStatus.INACTIVE, Instant.now(), Instant.now(), new ArrayList<>());
        when(vocabulary.findByNormalizedLemmas(List.of("live"))).thenReturn(List.of(pronunciationOnly));

        List<WordLookupResult> result = useCase().execute(List.of("Live"));

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.lemma()).isEqualTo("live");
            assertThat(found.ipa()).isEqualTo("/laɪv/");
            assertThat(found.senses()).isEmpty();
        });
    }

    @Test
    void returnsOnlyTheActiveSensesInOrder() {
        VocabularyItem item = VocabularyItem.create("Trend", "/trend/", null);
        item.addSense(sense(item, 2, ContentStatus.ACTIVE));
        item.addSense(sense(item, 1, ContentStatus.ACTIVE));
        item.addSense(sense(item, 0, ContentStatus.INACTIVE));
        when(vocabulary.findByNormalizedLemmas(List.of("trend"))).thenReturn(List.of(item));

        List<WordLookupResult> result = useCase().execute(List.of("trend"));

        assertThat(result).singleElement().satisfies(found ->
                assertThat(found.senses()).hasSize(2).allSatisfy(sense -> assertThat(sense.partOfSpeech()).isEqualTo("NOUN")));
    }

    @Test
    void keepsTheOrderOfTheFormsAskedFor() {
        VocabularyItem base = VocabularyItem.create("download", null, null);
        VocabularyItem plural = VocabularyItem.create("downloads", null, null);
        when(vocabulary.findByNormalizedLemmas(List.of("downloads", "download"))).thenReturn(List.of(base, plural));

        List<WordLookupResult> result = useCase().execute(List.of("downloads", "download", " "));

        assertThat(result).extracting(WordLookupResult::normalizedLemma).containsExactly("downloads", "download");
    }

    @Test
    void addsTheRecordedPronunciationOfEachWordThatHasOne() {
        VocabularyItem surface = VocabularyItem.create("surface", "/ˈsɜːfəs/", null);
        VocabularyItem silent = VocabularyItem.create("silent", null, null);
        when(vocabulary.findByNormalizedLemmas(List.of("surface", "silent"))).thenReturn(List.of(surface, silent));
        when(catalog.pronunciationAudio(List.of("surface", "silent"))).thenReturn(
                Map.of("surface", new WordAudioResult("https://media/uk/surface.mp3", "https://media/us/surface.mp3")));

        List<WordLookupResult> result = useCase().execute(List.of("surface", "silent"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).audioUk()).isEqualTo("https://media/uk/surface.mp3");
        assertThat(result.get(0).audioUs()).isEqualTo("https://media/us/surface.mp3");
        assertThat(result.get(1).audioUk()).isNull();
        assertThat(result.get(1).audioUs()).isNull();
    }

    private static VocabularySense sense(VocabularyItem item, int sortOrder, ContentStatus status) {
        return new VocabularySense(UUID.randomUUID(), item.getId(), PartOfSpeech.NOUN, "def", "nghĩa", "Example.",
                null, sortOrder, status, Instant.now(), Instant.now());
    }
}
