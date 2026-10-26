package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VocabularySenseCardResult;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import com.ieltspath.library.domain.vo.VocabularySenseCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetVocabularySensesUseCaseTest {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @InjectMocks
    private GetVocabularySensesUseCase useCase;

    @Test
    void keepsRequestedOrderAndDuplicateIdsWhileOmittingUnknownIds() {
        UUID firstId = UUID.fromString("10000000-0000-4000-8000-000000000001");
        UUID secondId = UUID.fromString("10000000-0000-4000-8000-000000000002");
        UUID unknownId = UUID.fromString("10000000-0000-4000-8000-000000000003");
        UUID itemId = UUID.fromString("20000000-0000-4000-8000-000000000001");
        List<UUID> requested = List.of(secondId, firstId, unknownId, secondId);

        when(vocabularyRepository.findActiveSensesByIds(requested)).thenReturn(List.of(
                new VocabularySenseCard(firstId, itemId, "resilient", "/rɪˈzɪliənt/", null,
                        PartOfSpeech.ADJECTIVE, "able to recover quickly", "kiên cường",
                        "The town proved resilient after the storm.", null),
                new VocabularySenseCard(secondId, itemId, "adapt", null, null, PartOfSpeech.VERB,
                        "to change to suit new conditions", "thích nghi",
                        "Plants adapt to dry conditions.", "https://example.test/plant.jpg")
        ));

        List<VocabularySenseCardResult> result = useCase.execute(requested);

        assertThat(result).extracting(VocabularySenseCardResult::senseId)
                .containsExactly(secondId, firstId, secondId);
        assertThat(result.get(0).lemma()).isEqualTo("adapt");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://example.test/plant.jpg");
        assertThat(result.get(1).pronunciationAudioReference()).isNull();
        verify(vocabularyRepository).findActiveSensesByIds(requested);
    }
}
