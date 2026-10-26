package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.InvalidKnowledgePointCodeException;
import com.ieltspath.content.domain.exception.KnowledgePointInUseException;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.exception.PrerequisiteLevelException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.service.KnowledgeGraph;
import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointCodeRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UpdateKnowledgePointUseCase {
    private final KnowledgePointRepository knowledgePointRepository;
    private final TopicRepository topicRepository;
    private final KnowledgePrerequisiteRepository prerequisiteRepository;
    private final TrapKnowledgePointSync trapKnowledgePointSync;
    private final KnowledgeGraph knowledgeGraph = new KnowledgeGraph();

    public UpdateKnowledgePointUseCase(KnowledgePointRepository knowledgePointRepository,
                                       TopicRepository topicRepository,
                                       KnowledgePrerequisiteRepository prerequisiteRepository,
                                       TrapKnowledgePointSync trapKnowledgePointSync) {
        this.knowledgePointRepository = knowledgePointRepository;
        this.topicRepository = topicRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.trapKnowledgePointSync = trapKnowledgePointSync;
    }

    public KnowledgePointResult execute(UUID id, UpdateKnowledgePointCommand command) {
        KnowledgePoint knowledgePoint = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new KnowledgePointNotFoundException(id));
        if (topicRepository.findById(command.topicId()).isEmpty()) {
            throw new TopicNotFoundException(command.topicId());
        }
        if (!knowledgePoint.getCode().equals(command.code())) {
            if (!KnowledgePointCodeRules.isValid(command.code(), command.kind(), command.skill())) {
                throw new InvalidKnowledgePointCodeException(command.code());
            }
            knowledgePointRepository.findByCode(command.code())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> { throw new DuplicateCodeException("KnowledgePoint", command.code()); });
        }
        if (!KnowledgePointCodeRules.isPrefixCompatible(command.code(), command.kind(), command.skill())) {
            throw new InvalidKnowledgePointCodeException(command.code());
        }
        if (!knowledgePoint.getTopicId().equals(command.topicId()) && knowledgePointRepository.isInUse(id)) {
            throw new KnowledgePointInUseException(id);
        }

        if (command.cefrLevel() != null && command.cefrLevel() != knowledgePoint.getCefrLevel()) {
            ensureLevelFitsEdges(id, command.cefrLevel());
        }

        knowledgePoint.update(command.topicId(), command.code(), command.name(), command.kind(),
                command.learningType(), command.skill(), command.description(), command.cefrLevel(), command.canDo(),
                command.scopeIn(), command.scopeOut());
        KnowledgePoint saved = knowledgePointRepository.save(knowledgePoint);
        trapKnowledgePointSync.syncForKnowledgePoint(id);
        return KnowledgePointResult.from(saved);
    }

    /** A new level must keep every prerequisite at or below it and every dependent at or above it. */
    private void ensureLevelFitsEdges(UUID id, CefrLevel level) {
        prerequisiteRepository.lockTree();
        List<KnowledgeGraph.Edge> touching = prerequisiteRepository.findAll().stream()
                .filter(edge -> edge.prerequisiteId().equals(id) || edge.knowledgePointId().equals(id))
                .map(edge -> new KnowledgeGraph.Edge(edge.prerequisiteId(), edge.knowledgePointId()))
                .toList();
        Set<UUID> neighbourIds = new HashSet<>();
        touching.forEach(edge -> {
            neighbourIds.add(edge.from());
            neighbourIds.add(edge.to());
        });
        neighbourIds.remove(id);
        Map<UUID, CefrLevel> levels = new HashMap<>();
        knowledgePointRepository.findByIds(List.copyOf(neighbourIds))
                .forEach(neighbour -> levels.put(neighbour.getId(), neighbour.getCefrLevel()));
        List<UUID> conflicts = knowledgeGraph.levelConflicts(id, level, touching, levels);
        if (!conflicts.isEmpty()) {
            throw new PrerequisiteLevelException(conflicts);
        }
    }
}
