package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.CreateVocabularyItemCommand;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
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
