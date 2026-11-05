package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularySuggestionsStore;
import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuggestVocabularyUseCaseTest {
    private final VocabularySuggestionsStore store = mock(VocabularySuggestionsStore.class);
    private final SuggestVocabularyUseCase useCase = new SuggestVocabularyUseCase(store);

    @Test void trimsAndNormalizesBeforeSearching() {
        var query = new VocabularySuggestionsQuery("  PoLL  ", 1, 20);
        var result = new VocabularySuggestionsResult(List.of(), 1, 20, false);
        when(store.search(query)).thenReturn(result);
        assertThat(useCase.execute(query)).isSameAs(result);
        verify(store).search(new VocabularySuggestionsQuery("poll", 1, 20));
    }

    @Test void emptyInputDoesNotReadTheCatalog() {
        assertThat(useCase.execute(new VocabularySuggestionsQuery("   ", 0, 20)).content()).isEmpty();
        verifyNoInteractions(store);
    }

    @Test void validatesBoundsAndKeepsLargeOffsetsFromOverflowing() {
        assertThatThrownBy(() -> new VocabularySuggestionsQuery("x".repeat(101), 0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VocabularySuggestionsQuery("poll", -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VocabularySuggestionsQuery("poll", 0, 51)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VocabularySuggestionsQuery("poll", 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new VocabularySuggestionsQuery("poll", Integer.MAX_VALUE, 50).offset()).isEqualTo(107374182350L);
    }
}
