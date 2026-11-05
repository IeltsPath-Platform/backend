package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseResult;
import com.ieltspath.library.application.port.PronunciationAudioUrls;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The active words whose lemma is exactly one of those asked for, each with its active senses in order. */
@Service
@Transactional(readOnly = true)
public class FindVocabularyByLemmasUseCase {

    private final VocabularyRepository vocabulary;
    private final PronunciationAudioUrls audioUrls;

    public FindVocabularyByLemmasUseCase(VocabularyRepository vocabulary, PronunciationAudioUrls audioUrls) {
        this.vocabulary = vocabulary;
        this.audioUrls = audioUrls;
    }

    public List<VocabularyItemResult> execute(List<String> lemmas) {
        List<String> keys = lemmas.stream().map(lemma -> lemma.trim().toLowerCase(Locale.ROOT))
                .filter(lemma -> !lemma.isEmpty()).distinct().toList();
        return vocabulary.findByNormalizedLemmas(keys).stream()
                .filter(item -> item.getStatus() == ContentStatus.ACTIVE)
                .map(this::toResult)
                .filter(item -> !item.senses().isEmpty())
                .sorted(Comparator.comparing(VocabularyItemResult::lemma))
                .toList();
    }

    private VocabularyItemResult toResult(VocabularyItem item) {
        List<VocabularySenseResult> senses = item.getSenses().stream()
                .filter(sense -> sense.getStatus() == ContentStatus.ACTIVE)
                .sorted(Comparator.comparingInt(VocabularySense::getSortOrder))
                .map(sense -> new VocabularySenseResult(sense.getId(), sense.getVocabularyItemId(),
                        sense.getPartOfSpeech(), sense.getEnglishDefinition(), sense.getVietnameseMeaning(),
                        sense.getExampleSentence(), sense.getImageUrl(), sense.getSortOrder(), sense.getStatus(),
                        sense.getCreatedAt(), sense.getUpdatedAt()))
                .toList();
        return new VocabularyItemResult(item.getId(), item.getLemma(), item.getNormalizedLemma(), item.getIpa(),
                audioUrls.resolve(item.preferredAudioReference()), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(),
                senses);
    }
}
