package com.group01.learning.application.usecase;

import com.group01.learning.application.result.MasteryResult;
import com.group01.learning.domain.repository.KnowledgeEvidenceRepository;
import com.group01.learning.domain.service.MasteryCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetMasteryUseCase {
    private final KnowledgeEvidenceRepository evidence;
    private final MasteryCalculator calculator = new MasteryCalculator();

    public GetMasteryUseCase(KnowledgeEvidenceRepository evidence) { this.evidence = evidence; }

    @Transactional(readOnly = true)
    public List<MasteryResult> execute(UUID userId) {
        return evidence.findMasteryHistories(userId).stream().map(history -> new MasteryResult(
                history.knowledgePointId(), history.topicId(), calculator.compute(history.correctness()),
                history.evidenceCount())).toList();
    }
}
