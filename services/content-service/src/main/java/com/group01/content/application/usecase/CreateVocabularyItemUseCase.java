package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateVocabularyItemCommand;
import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CreateVocabularyItemUseCase {

    private final VocabularyRepository vocabularyRepository;

    public CreateVocabularyItemUseCase(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public VocabularyItemResult execute(CreateVocabularyItemCommand command) {
        String normalized = command.lemma().trim().toLowerCase(Locale.ROOT);
        if (vocabularyRepository.existsByNormalizedLemma(normalized)) {
            throw new DuplicateCodeException("VocabularyItem", command.lemma());
        }

        VocabularyItem item = VocabularyItem.create(
                command.lemma(),
                command.ipa(),
                command.pronunciationAudioReference()
        );

        VocabularyItem saved = vocabularyRepository.save(item);
        return new VocabularyItemResult(
                saved.getId(),
                saved.getLemma(),
                saved.getNormalizedLemma(),
                saved.getIpa(),
                saved.getPronunciationAudioReference(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                List.of()
        );
    }
}

