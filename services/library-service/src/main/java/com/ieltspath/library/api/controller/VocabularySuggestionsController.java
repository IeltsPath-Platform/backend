package com.ieltspath.library.api.controller;

import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import com.ieltspath.library.application.usecase.SuggestVocabularyUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/admin/vocabulary/suggestions")
@PreAuthorize("hasAnyRole('CONTENT_AUTHOR', 'ADMIN')")
public class VocabularySuggestionsController {
    private final SuggestVocabularyUseCase suggest;

    public VocabularySuggestionsController(SuggestVocabularyUseCase suggest) {
        this.suggest = suggest;
    }

    @GetMapping
    public VocabularySuggestionsResult suggestions(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return suggest.execute(new VocabularySuggestionsQuery(q, page, size));
    }
}
