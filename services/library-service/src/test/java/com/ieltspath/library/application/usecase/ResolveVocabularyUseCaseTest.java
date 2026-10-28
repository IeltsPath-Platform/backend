package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.ResolveVocabularyCommand;
import com.ieltspath.library.application.command.ResolveVocabularyCommand.Entry;
import com.ieltspath.library.application.result.ResolvedVocabularyResult;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.domain.entity.VocabularySense;
import com.ieltspath.library.domain.exception.InvalidContentStateException;
import com.ieltspath.library.domain.repository.VocabularyRepository;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveVocabularyUseCaseTest {

    @Mock
    private VocabularyRepository vocabulary;

    private ResolveVocabularyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveVocabularyUseCase(vocabulary);
    }

    private static Entry entry(String lemma) {
        return new Entry(lemma, "/ipa/", PartOfSpeech.VERB, "to vary", "dao động", "Prices fluctuate.");
    }

    @Test
    void createsTheWordAndItsSenseWhenTheLibraryLacksThem() {
        when(vocabulary.findByNormalizedLemmas(List.of("fluctuate"))).thenReturn(List.of());
        when(vocabulary.save(any(VocabularyItem.class))).thenAnswer(call -> call.getArgument(0));

        List<ResolvedVocabularyResult> result = useCase.execute(new ResolveVocabularyCommand(List.of(entry("Fluctuate "))));

        assertThat(result).singleElement().satisfies(resolved -> {
            assertThat(resolved.lemma()).isEqualTo("fluctuate");
            assertThat(resolved.created()).isTrue();
            assertThat(resolved.senseId()).isNotNull();
        });
        // Once for the new item, once more with its sense.
        verify(vocabulary, org.mockito.Mockito.times(2)).save(any(VocabularyItem.class));
    }

    @Test
    void keepsTheFirstActiveSenseOfAWordTheLibraryAlreadyHas() {
        VocabularyItem item = VocabularyItem.create("fluctuate", null, null);
        UUID later = senseOf(item, 2, ContentStatus.ACTIVE);
        UUID first = senseOf(item, 1, ContentStatus.ACTIVE);
        senseOf(item, 0, ContentStatus.INACTIVE);
        when(vocabulary.findByNormalizedLemmas(List.of("fluctuate"))).thenReturn(List.of(item));

        List<ResolvedVocabularyResult> result = useCase.execute(new ResolveVocabularyCommand(List.of(entry("fluctuate"))));

        assertThat(result).singleElement().satisfies(resolved -> {
            assertThat(resolved.created()).isFalse();
            assertThat(resolved.senseId()).isEqualTo(first).isNotEqualTo(later);
        });
        verify(vocabulary, never()).save(any(VocabularyItem.class));
    }

    @Test
    void addsASenseToAnActiveWordThatHasNoneLeft() {
        VocabularyItem item = VocabularyItem.create("fluctuate", null, null);
        senseOf(item, 0, ContentStatus.INACTIVE);
        when(vocabulary.findByNormalizedLemmas(List.of("fluctuate"))).thenReturn(List.of(item));
        when(vocabulary.save(any(VocabularyItem.class))).thenAnswer(call -> call.getArgument(0));

        List<ResolvedVocabularyResult> result = useCase.execute(new ResolveVocabularyCommand(List.of(entry("fluctuate"))));

        assertThat(result).singleElement().satisfies(resolved -> assertThat(resolved.created()).isTrue());
        verify(vocabulary).save(item);
    }

    @Test
    void refusesAWordThatIsSwitchedOff() {
        VocabularyItem inactive = new VocabularyItem(UUID.randomUUID(), "going", "going", null, null,
                ContentStatus.INACTIVE, Instant.now(), Instant.now(), new ArrayList<>());
        when(vocabulary.findByNormalizedLemmas(List.of("going"))).thenReturn(List.of(inactive));

        assertThatThrownBy(() -> useCase.execute(new ResolveVocabularyCommand(List.of(entry("going")))))
                .isInstanceOf(InvalidContentStateException.class);
        verify(vocabulary, never()).save(any(VocabularyItem.class));
    }

    @Test
    void resolvesALemmaThatAppearsTwiceOnce() {
        when(vocabulary.findByNormalizedLemmas(List.of("trend"))).thenReturn(List.of());
        when(vocabulary.save(any(VocabularyItem.class))).thenAnswer(call -> call.getArgument(0));

        List<ResolvedVocabularyResult> result = useCase.execute(
                new ResolveVocabularyCommand(List.of(entry("trend"), entry("TREND"))));

        assertThat(result).hasSize(1);
        verify(vocabulary, org.mockito.Mockito.times(2)).save(any(VocabularyItem.class));
    }

    private static UUID senseOf(VocabularyItem item, int sortOrder, ContentStatus status) {
        VocabularySense sense = new VocabularySense(UUID.randomUUID(), item.getId(), PartOfSpeech.VERB, "def", "nghĩa",
                "Example.", null, sortOrder, status, Instant.now(), Instant.now());
        item.addSense(sense);
        return sense.getId();
    }
}
