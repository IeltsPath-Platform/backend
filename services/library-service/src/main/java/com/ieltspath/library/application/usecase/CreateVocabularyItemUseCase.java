package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.CreateVocabularyItemCommand;
import com.ieltspath.library.application.port.PronunciationAudioUrls;
import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.result.VocabularySenseResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Adds a word to the library. A word that is already active is refused. A word kept INACTIVE (most are the pronunciation
 * rows of the dictation videos, such as "grammar") is switched on instead: it never shows in the suggestions, so an
 * author could not find it and could not create it either. It keeps the pronunciation and dictionary data it holds;
 * what the author gives fills the fields, and an empty field keeps the stored value.
 */
@Service
@Transactional
public class CreateVocabularyItemUseCase {

    private final VocabularyRepository vocabularyRepository;
    private final VocabularyCatalogStore catalogStore;
    private final PronunciationAudioUrls audioUrls;

    public CreateVocabularyItemUseCase(VocabularyRepository vocabularyRepository, VocabularyCatalogStore catalogStore,
                                       PronunciationAudioUrls audioUrls) {
        this.vocabularyRepository = vocabularyRepository;
        this.catalogStore = catalogStore;
        this.audioUrls = audioUrls;
    }

    public VocabularyItemResult execute(CreateVocabularyItemCommand command) {
        String normalized = command.lemma().trim().toLowerCase(Locale.ROOT);
        Optional<VocabularyItem> existing = vocabularyRepository.findByNormalizedLemma(normalized);
        VocabularyItemDetails dictionary = command.dictionary();
        VocabularyItem saved;
        if (existing.isPresent()) {
            VocabularyItem item = existing.get();
            if (item.getStatus() == ContentStatus.ACTIVE) {
                throw new DuplicateCodeException("VocabularyItem", command.lemma());
            }
            item.update(command.lemma(), or(command.ipa(), item.getIpa()),
                    or(command.pronunciationAudioReference(), item.getPronunciationAudioReference()), ContentStatus.ACTIVE);
            saved = vocabularyRepository.save(item);
            VocabularyItemDetails given = dictionary;
            dictionary = catalogStore.findItemDetails(saved.getId()).map(kept -> merge(given, kept)).orElse(given);
        } else {
            saved = vocabularyRepository.save(VocabularyItem.create(command.lemma(), command.ipa(),
                    command.pronunciationAudioReference()));
        }
        // The dictionary columns are written by the catalog store, not the entity mapping. The save above is flushed
        // first, so the row exists for this update in the same transaction.
        catalogStore.updateItem(saved.getId(), dictionary).orElseThrow(ResourceNotFoundException::new);
        return new VocabularyItemResult(
                saved.getId(),
                saved.getLemma(),
                saved.getNormalizedLemma(),
                saved.getIpa(),
                // The US/UK audio was just written by the catalog store, not the entity, so pick from what was stored.
                audioUrls.resolve(VocabularyItem.preferredAudioReference(dictionary.audioUsReference(),
                        dictionary.audioUkReference(), saved.getPronunciationAudioReference())),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                // A switched-on word may already have meanings; the editor needs them to tell its new one apart.
                saved.getSenses().stream().map(s -> new VocabularySenseResult(s.getId(), s.getVocabularyItemId(),
                        s.getPartOfSpeech(), s.getEnglishDefinition(), s.getVietnameseMeaning(), s.getExampleSentence(),
                        s.getImageUrl(), s.getSortOrder(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt())).toList()
        );
    }

    /** What the author gave, field by field, with the stored value where the author left a field empty. */
    private static VocabularyItemDetails merge(VocabularyItemDetails given, VocabularyItemDetails kept) {
        return new VocabularyItemDetails(or(given.ipaUk(), kept.ipaUk()), or(given.ipaUs(), kept.ipaUs()),
                or(given.audioUkReference(), kept.audioUkReference()), or(given.audioUsReference(), kept.audioUsReference()),
                kept.frequencyRank(), kept.academicWordList(), kept.wordFamily());
    }

    private static String or(String given, String kept) {
        return given != null && !given.isBlank() ? given : kept;
    }
}