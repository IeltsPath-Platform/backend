package com.group01.content.domain.repository;

import com.group01.content.domain.entity.QuestionKnowledgePoint;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Read access to the canonical question-version to knowledge-point mapping. */
public interface QuestionKnowledgePointRepository {
    List<QuestionKnowledgePoint> findByQuestionVersionIds(Collection<UUID> questionVersionIds);
}
