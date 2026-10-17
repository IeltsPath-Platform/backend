package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.SuggestionCandidateRepository;
import com.ieltspath.content.application.result.SuggestionCandidatesResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class GetSuggestionCandidatesUseCase {
    private final SuggestionCandidateRepository repository;

    public GetSuggestionCandidatesUseCase(SuggestionCandidateRepository repository) {
        this.repository = repository;
    }

    public SuggestionCandidatesResult execute(List<UUID> knowledgePointIds, BigDecimal maxBand) {
        return new SuggestionCandidatesResult(repository.packages(knowledgePointIds, maxBand),
                repository.lessons(knowledgePointIds, maxBand));
    }
}
