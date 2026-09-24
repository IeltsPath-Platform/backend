package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.ErrorAnalysisItem;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ErrorAnalysisItemRepository {
    List<ErrorAnalysisItem> saveAll(List<ErrorAnalysisItem> values);

    List<ErrorAnalysisItem> findByResultId(UUID resultId);

    List<ErrorAnalysisItem> findByIds(Set<UUID> ids);
}
