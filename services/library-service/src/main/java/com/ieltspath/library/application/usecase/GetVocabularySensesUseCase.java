package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularySenseCardResult;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetVocabularySensesUseCase {

    private final VocabularyRepository vocabularyRepository;

    public GetVocabularySensesUseCase(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    public List<VocabularySenseCardResult> execute(List<UUID> senseIds) {
        Map<UUID, VocabularySenseCard> cardsById = new HashMap<>();
        for (VocabularySenseCard card : vocabularyRepository.findActiveSensesByIds(senseIds)) {
            cardsById.put(card.senseId(), card);
        }

        return senseIds.stream()
                .map(cardsById::get)
                .filter(card -> card != null)
                .map(card -> new VocabularySenseCardResult(
                        card.senseId(),
                        card.vocabularyItemId(),
                        card.lemma(),
                        card.ipa(),
                        card.pronunciationAudioReference(),
                        card.partOfSpeech(),
                        card.englishDefinition(),
                        card.vietnameseMeaning(),
                        card.exampleSentence(),
                        card.imageUrl()
                ))
                .toList();
    }
}
