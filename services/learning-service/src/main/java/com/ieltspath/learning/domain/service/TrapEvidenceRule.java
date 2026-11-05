package com.ieltspath.learning.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Maps an answered question's tagged distractors to evidence for their trap knowledge points. */
public final class TrapEvidenceRule {
    private TrapEvidenceRule() {}

    public static List<TrapOutcome> outcomes(Map<String, Object> answerSpec, boolean correct, String chosenErrorCode,
                                             Map<String, UUID> trapKpByCode) {
        if (answerSpec == null || trapKpByCode == null || trapKpByCode.isEmpty()) return List.of();
        Object raw = answerSpec.get("distractors");
        if (!(raw instanceof Map<?, ?> distractors)) return List.of();
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (Object value : distractors.values()) if (value instanceof String code && !code.isBlank()) codes.add(code);
        if (correct) return codes.stream().map(trapKpByCode::get).filter(java.util.Objects::nonNull)
                .map(kp -> new TrapOutcome(kp, true)).toList();
        if (chosenErrorCode == null || !codes.contains(chosenErrorCode)) return List.of();
        UUID kpId = trapKpByCode.get(chosenErrorCode);
        return kpId == null ? List.of() : List.of(new TrapOutcome(kpId, false));
    }

    public record TrapOutcome(UUID trapKnowledgePointId, boolean correct) {}
}
