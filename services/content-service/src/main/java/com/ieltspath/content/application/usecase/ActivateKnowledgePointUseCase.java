package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ActivateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;

    public ActivateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
    }

    public KnowledgePointResult execute(UUID id) {
        var knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        knowledgePoint.activate();
        return KnowledgePointResult.from(knowledgePointRepository.save(knowledgePoint));
    }
}
