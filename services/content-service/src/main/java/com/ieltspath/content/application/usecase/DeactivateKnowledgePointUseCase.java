package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeactivateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;

    public DeactivateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository) {
        this.knowledgePointRepository = knowledgePointRepository;
    }

    public void execute(UUID id) {
        var knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        knowledgePoint.deactivate();
        knowledgePointRepository.save(knowledgePoint);
    }
}
