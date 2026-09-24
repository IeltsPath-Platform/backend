package com.group01.content.application.usecase;

import com.group01.content.application.result.QuestionKnowledgePointResult;
import com.group01.content.domain.repository.QuestionKnowledgePointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Returns the canonical question-version to knowledge-point mappings so another service can snapshot them.
 * Content remains the owner; callers keep a copy for the lifetime of their own record.
 */
@Service
@Transactional(readOnly = true)
public class GetQuestionKnowledgePointMappingsUseCase {
    public static final int MAX_QUESTION_VERSIONS = 500;

    private final QuestionKnowledgePointRepository mappings;

    public GetQuestionKnowledgePointMappingsUseCase(QuestionKnowledgePointRepository mappings) {
        this.mappings = mappings;
    }

    public List<QuestionKnowledgePointResult> execute(List<UUID> questionVersionIds) {
        if (questionVersionIds == null || questionVersionIds.isEmpty()) {
            throw new IllegalArgumentException("questionVersionIds must not be empty");
        }
        if (questionVersionIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("questionVersionIds must not contain null");
        }
        var unique = new HashSet<>(questionVersionIds);
        if (unique.size() > MAX_QUESTION_VERSIONS) {
            throw new IllegalArgumentException("At most " + MAX_QUESTION_VERSIONS + " question versions can be requested");
        }
        return mappings.findByQuestionVersionIds(unique).stream()
                .map(mapping -> new QuestionKnowledgePointResult(
                        mapping.getQuestionVersionId(), mapping.getKnowledgePointId(), mapping.getWeight()))
                .toList();
    }
}
