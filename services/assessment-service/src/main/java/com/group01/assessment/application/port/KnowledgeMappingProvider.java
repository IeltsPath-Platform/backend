package com.group01.assessment.application.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads the canonical question-version to knowledge-point mapping owned by Content Service. */
public interface KnowledgeMappingProvider {
    /** Question versions without a mapping are absent from the returned map. */
    Map<UUID, List<KnowledgePointWeight>> findByQuestionVersionIds(Collection<UUID> questionVersionIds);

    record KnowledgePointWeight(UUID knowledgePointId, BigDecimal weight) {}
}
