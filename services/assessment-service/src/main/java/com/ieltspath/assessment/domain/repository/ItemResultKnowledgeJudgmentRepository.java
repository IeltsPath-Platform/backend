package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.entity.ItemResultKnowledgeJudgment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ItemResultKnowledgeJudgmentRepository {
    List<ItemResultKnowledgeJudgment> saveAll(List<ItemResultKnowledgeJudgment> values);

    List<ItemResultKnowledgeJudgment> findByItemResultIds(Collection<UUID> itemResultIds);
}
