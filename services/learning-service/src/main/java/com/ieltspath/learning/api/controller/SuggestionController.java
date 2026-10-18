package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.result.SuggestionResult;
import com.ieltspath.learning.application.usecase.GetSuggestionsUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/suggestions")
@PreAuthorize("hasRole('CUSTOMER')")
public class SuggestionController {
    private final CurrentUserProvider currentUser;
    private final GetSuggestionsUseCase suggestions;

    public SuggestionController(CurrentUserProvider currentUser, GetSuggestionsUseCase suggestions) {
        this.currentUser = currentUser;
        this.suggestions = suggestions;
    }

    @GetMapping
    public SuggestionResult mine() {
        return suggestions.execute(currentUser.requireUserId());
    }
}
