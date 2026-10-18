package com.ieltspath.content.application.port;

import com.ieltspath.content.application.result.SuggestionCandidatesResult;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SuggestionCandidateRepository {
    List<SuggestionCandidatesResult.PackageCandidate> packages(Collection<UUID> knowledgePointIds, BigDecimal maxBand);

    List<SuggestionCandidatesResult.LessonCandidate> lessons(Collection<UUID> knowledgePointIds, BigDecimal maxBand);
}
