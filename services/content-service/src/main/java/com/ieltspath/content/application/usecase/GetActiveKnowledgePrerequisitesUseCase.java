package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetActiveKnowledgePrerequisitesUseCase {
    private final LearningContentReader learningContentReader;

    public GetActiveKnowledgePrerequisitesUseCase(LearningContentReader learningContentReader) {
        this.learningContentReader = learningContentReader;
    }

    public Map<UUID, List<UUID>> execute(Collection<UUID> ids) {
        return learningContentReader.activeKnowledgePrerequisites(ids);
    }
}
