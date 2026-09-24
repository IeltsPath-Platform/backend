package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.repository.QuestionKnowledgePointRepository;
import com.group01.content.infrastructure.persistence.repository.QuestionKnowledgePointJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class QuestionKnowledgePointRepositoryAdapter implements QuestionKnowledgePointRepository {

    private final QuestionKnowledgePointJpaRepository repository;

    public QuestionKnowledgePointRepositoryAdapter(QuestionKnowledgePointJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<QuestionKnowledgePoint> findByQuestionVersionIds(Collection<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) {
            return List.of();
        }
        return repository.findByQuestionVersionIds(questionVersionIds).stream()
                .map(mapping -> new QuestionKnowledgePoint(
                        mapping.getId().getQuestionVersionId(),
                        mapping.getId().getKnowledgePointId(),
                        mapping.getWeight()))
                .toList();
    }
}
