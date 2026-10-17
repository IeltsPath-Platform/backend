package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.internal.SuggestionCandidatesRequest;
import com.ieltspath.content.application.result.SuggestionCandidatesResult;
import com.ieltspath.content.application.usecase.GetSuggestionCandidatesUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/learning-content")
public class InternalSuggestionController {
    private final GetSuggestionCandidatesUseCase getCandidates;

    public InternalSuggestionController(GetSuggestionCandidatesUseCase getCandidates) {
        this.getCandidates = getCandidates;
    }

    @PostMapping("/suggestions/candidates")
    public SuggestionCandidatesResult candidates(@Valid @RequestBody SuggestionCandidatesRequest request) {
        return getCandidates.execute(request.knowledgePointIds(), request.maxBand());
    }
}
