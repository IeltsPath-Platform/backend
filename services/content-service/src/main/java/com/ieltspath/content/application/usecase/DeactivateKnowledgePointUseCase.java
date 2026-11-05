package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeactivateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final TrapKnowledgePointSync trapKnowledgePointSync;

    public DeactivateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                           TrapKnowledgePointSync trapKnowledgePointSync) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.trapKnowledgePointSync = trapKnowledgePointSync;
    }

    public void execute(UUID id) {
        var knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        knowledgePoint.deactivate();
        knowledgePointRepository.save(knowledgePoint);
        trapKnowledgePointSync.syncForKnowledgePoint(id);
    }
}
