package com.group01.content.application.usecase;

import com.group01.content.application.command.AddVocabularySenseCommand;
import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.application.result.VocabularySenseResult;
import com.group01.content.domain.aggregate.VocabularyItem;
import com.group01.content.domain.entity.VocabularySense;
import com.group01.content.domain.exception.VocabularyNotFoundException;
import com.group01.content.domain.repository.VocabularyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddVocabularySenseUseCase {

    private final VocabularyRepository vocabularyRepository;

    public AddVocabularySenseUseCase(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public VocabularyItemResult execute(AddVocabularySenseCommand command) {
        VocabularyItem item = vocabularyRepository.findById(command.vocabularyItemId())
                .orElseThrow(() -> new VocabularyNotFoundException(command.vocabularyItemId()));

        VocabularySense sense = VocabularySense.create(
                command.vocabularyItemId(),
                command.partOfSpeech(),
                command.englishDefinition(),
                command.vietnameseMeaning(),
                command.exampleSentence(),
                command.imageUrl(),
                command.sortOrder()
        );

        item.addSense(sense);
        VocabularyItem saved = vocabularyRepository.save(item);

        List<VocabularySenseResult> senseResults = saved.getSenses().stream()
                .map(s -> new VocabularySenseResult(
                        s.getId(),
                        s.getVocabularyItemId(),
                        s.getPartOfSpeech(),
                        s.getEnglishDefinition(),
                        s.getVietnameseMeaning(),
                        s.getExampleSentence(),
                        s.getImageUrl(),
                        s.getSortOrder(),
                        s.getStatus(),
                        s.getCreatedAt(),
                        s.getUpdatedAt()
                ))
                .toList();

        return new VocabularyItemResult(
                saved.getId(),
                saved.getLemma(),
                saved.getNormalizedLemma(),
                saved.getIpa(),
                saved.getPronunciationAudioReference(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                senseResults
        );
    }
}

