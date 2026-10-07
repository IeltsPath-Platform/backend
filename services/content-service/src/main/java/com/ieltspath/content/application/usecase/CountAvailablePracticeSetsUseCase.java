package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CountPracticeSetsCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** How many eligible practice sets remain for each knowledge point once the given packages are left out. */
@Service
@Transactional(readOnly = true)
public class CountAvailablePracticeSetsUseCase {
    static final int MAX_KNOWLEDGE_POINTS = 50;

    private final LearningContentReader reader;

    public CountAvailablePracticeSetsUseCase(LearningContentReader reader) {
        this.reader = reader;
    }

    public Map<UUID, Integer> execute(CountPracticeSetsCommand command) {
        List<UUID> knowledgePoints = command.knowledgePointIds();
        if (knowledgePoints == null || knowledgePoints.isEmpty() || knowledgePoints.size() > MAX_KNOWLEDGE_POINTS) {
            throw new IllegalArgumentException("knowledgePointIds must hold 1 to " + MAX_KNOWLEDGE_POINTS + " ids");
        }
        int minQuestions = command.minQuestions() == null
                ? SearchPracticeSetsUseCase.DEFAULT_MIN_QUESTIONS : command.minQuestions();
        if (minQuestions < 1) {
            throw new IllegalArgumentException("minQuestions must be at least 1");
        }
        List<UUID> exclude = command.excludePackageIds() == null ? List.of() : command.excludePackageIds();
        return reader.countEligiblePracticeSets(knowledgePoints.stream().distinct().toList(), exclude, minQuestions);
    }
}
