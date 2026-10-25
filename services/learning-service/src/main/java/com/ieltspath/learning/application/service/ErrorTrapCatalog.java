package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The local mapping from error codes to trap KPs; intentionally makes no service-to-service request. */
@Component
public class ErrorTrapCatalog {
    private final KnowledgePointCatalogRepository catalog;

    public ErrorTrapCatalog(KnowledgePointCatalogRepository catalog) { this.catalog = catalog; }

    public ErrorTraps load() {
        Map<String, UUID> byCode = new LinkedHashMap<>();
        Map<UUID, UUID> parentByTrap = new LinkedHashMap<>();
        Map<UUID, List<UUID>> byParent = new LinkedHashMap<>();
        for (KnowledgePointCatalogEntry entry : catalog.findAll()) {
            if (entry.parentKnowledgePointId() == null) continue;
            parentByTrap.put(entry.knowledgePointId(), entry.parentKnowledgePointId());
            byParent.computeIfAbsent(entry.parentKnowledgePointId(), ignored -> new ArrayList<>())
                    .add(entry.knowledgePointId());
            if (entry.errorCode() != null) byCode.put(entry.errorCode(), entry.knowledgePointId());
        }
        Map<UUID, List<UUID>> immutable = new LinkedHashMap<>();
        byParent.forEach((parent, children) -> immutable.put(parent, List.copyOf(children)));
        return new ErrorTraps(Map.copyOf(byCode), Map.copyOf(parentByTrap), Map.copyOf(immutable));
    }

    public record ErrorTraps(Map<String, UUID> trapKpByCode, Map<UUID, UUID> parentByTrapKp,
                             Map<UUID, List<UUID>> trapKpsByParent) {}
}
