package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.application.result.WordAudioResult;
import com.ieltspath.library.application.result.WordLookupResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Looks words up by exact lemma for a learner who clicked a word. Unlike the search, it also finds words that are not
 * active: those are pronunciation entries of the dictation words and have no sense, so they come back with their
 * pronunciation only. The result follows the order of the forms asked for, so the caller can take the first one it can use.
 */
@Service
@Transactional(readOnly = true)
public class LookupVocabularyWordUseCase {

    private static final WordAudioResult NO_AUDIO = new WordAudioResult(null, null);

    private final VocabularyRepository vocabulary;
    private final VocabularyCatalogStore catalog;

    public LookupVocabularyWordUseCase(VocabularyRepository vocabulary, VocabularyCatalogStore catalog) {
        this.vocabulary = vocabulary;
        this.catalog = catalog;
    }

    public List<WordLookupResult> execute(List<String> forms) {
        List<String> keys = forms.stream().map(form -> form.trim().toLowerCase(Locale.ROOT))
                .filter(form -> !form.isEmpty()).distinct().toList();
        Map<String, WordAudioResult> audio = catalog.pronunciationAudio(keys);
        return vocabulary.findByNormalizedLemmas(keys).stream()
                .map(item -> toResult(item, audio.getOrDefault(item.getNormalizedLemma(), NO_AUDIO)))
                .sorted(Comparator.comparingInt(result -> keys.indexOf(result.normalizedLemma())))
                .toList();
    }

    private static WordLookupResult toResult(VocabularyItem item, WordAudioResult audio) {
        List<WordLookupResult.Sense> senses = item.getSenses().stream()
                .filter(sense -> sense.getStatus() == ContentStatus.ACTIVE)
                .sorted(Comparator.comparingInt(VocabularySense::getSortOrder))
                .map(sense -> new WordLookupResult.Sense(
                        sense.getPartOfSpeech() == null ? null : sense.getPartOfSpeech().name(),
                        sense.getEnglishDefinition(), sense.getVietnameseMeaning(), sense.getExampleSentence()))
                .toList();
        return new WordLookupResult(item.getLemma(), item.getNormalizedLemma(), item.getIpa(), audio.audioUk(),
                audio.audioUs(), senses);
    }
}
