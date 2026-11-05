package com.ieltspath.content.application.service;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Keeps the evidence-only knowledge-point child aligned with its error type and parent. */
@Service
@Transactional
public class TrapKnowledgePointSync {
    private final KnowledgePointRepository knowledgePoints;
    private final ErrorTypeRepository errorTypes;

    public TrapKnowledgePointSync(KnowledgePointRepository knowledgePoints, ErrorTypeRepository errorTypes) {
        this.knowledgePoints = knowledgePoints;
        this.errorTypes = errorTypes;
    }

    public void sync(ErrorType errorType) {
        var childCode = "TRAP_" + errorType.getCode();
        var existingChild = knowledgePoints.findByCode(childCode);
        var parent = errorType.getKnowledgePointId() == null
                ? null : knowledgePoints.findById(errorType.getKnowledgePointId()).orElse(null);
        boolean active = errorType.getStatus() == ContentStatus.ACTIVE
                && parent != null && parent.getStatus() == ContentStatus.ACTIVE;

        if (active) {
            KnowledgePoint child = existingChild.orElseGet(() -> createChild(errorType, parent));
            child.update(parent.getTopicId(), childCode, errorType.getName(), parent.getKind(),
                    parent.getLearningType(), parent.getSkill(), errorType.getFixHint(), parent.getCefrLevel(),
                    parent.getCanDo(), parent.getScopeIn(), parent.getScopeOut());
            if (child.getStatus() != ContentStatus.ACTIVE) {
                child.activate();
            }
            errorType.setTrapKnowledgePointId(knowledgePoints.save(child).getId());
            return;
        }

        existingChild.ifPresent(child -> {
            child.deactivate();
            errorType.setTrapKnowledgePointId(knowledgePoints.save(child).getId());
        });
        if (existingChild.isEmpty()) {
            errorType.setTrapKnowledgePointId(null);
        }
    }

    public void syncForKnowledgePoint(UUID knowledgePointId) {
        errorTypes.findByKnowledgePointId(knowledgePointId).forEach(errorType -> {
            sync(errorType);
            errorTypes.save(errorType);
        });
    }

    private static KnowledgePoint createChild(ErrorType errorType, KnowledgePoint parent) {
        return KnowledgePoint.create(parent.getTopicId(), "TRAP_" + errorType.getCode(), errorType.getName(),
                parent.getKind(), parent.getLearningType(), parent.getSkill(), errorType.getFixHint(),
                parent.getCefrLevel(), parent.getCanDo(), parent.getScopeIn(), parent.getScopeOut());
    }
}
