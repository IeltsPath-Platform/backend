package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularySuggestionsStore;
import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SuggestVocabularyUseCase {
    private final VocabularySuggestionsStore store;

    public SuggestVocabularyUseCase(VocabularySuggestionsStore store) {
        this.store = store;
    }

    @Transactional(readOnly = true, timeout = 2)
    public VocabularySuggestionsResult execute(VocabularySuggestionsQuery query) {
        if (query.text().isEmpty()) {
            return new VocabularySuggestionsResult(List.of(), query.page(), query.size(), false);
        }
        return store.search(query);
    }
}
