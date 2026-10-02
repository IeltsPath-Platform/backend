package com.group01.learning.application.usecase;

import com.group01.learning.application.port.LearningProgressStore;
import com.group01.learning.application.result.MasteryResult;
import com.group01.learning.domain.service.MasteryCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetMasteryUseCase {
    private final LearningProgressStore store;
    private final MasteryCalculator calculator = new MasteryCalculator();

    public GetMasteryUseCase(LearningProgressStore store) { this.store = store; }

    @Transactional(readOnly = true)
    public List<MasteryResult> execute(UUID userId) {
        return store.findMastery(userId).stream().map(history -> new MasteryResult(
                history.knowledgePointId(), history.topicId(), calculator.compute(history.correctness()),
                history.evidenceCount())).toList();
    }
}
