package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ActivateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final TrapKnowledgePointSync trapKnowledgePointSync;

    public ActivateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                         TrapKnowledgePointSync trapKnowledgePointSync) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.trapKnowledgePointSync = trapKnowledgePointSync;
    }

    public KnowledgePointResult execute(UUID id) {
        var knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        knowledgePoint.activate();
        var saved = knowledgePointRepository.save(knowledgePoint);
        trapKnowledgePointSync.syncForKnowledgePoint(id);
        return KnowledgePointResult.from(saved);
    }
}
