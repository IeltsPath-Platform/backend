package com.ieltspath.library.application.port;

import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;

public interface VocabularySuggestionsStore {
    VocabularySuggestionsResult search(VocabularySuggestionsQuery query);
}
