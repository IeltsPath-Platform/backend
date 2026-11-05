package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Applies only verified criterion bands to their mapped knowledge points after payment has completed. */
@Component
public class SpeakingPathUpdater {
    private final KnowledgeEvidenceRepository evidence;
    private final MasteryProfileUpdater mastery;

    public SpeakingPathUpdater(KnowledgeEvidenceRepository evidence, MasteryProfileUpdater mastery) {
        this.evidence = evidence;
        this.mastery = mastery;
    }

    @SuppressWarnings("unchecked")
    public void apply(UUID userId, UUID submissionId, LearningContentClient.Rubric rubric,
                      Map<String, Object> signals, BigDecimal passBand) {
        if (signals == null || !Boolean.TRUE.equals(signals.get("usableAsEvidence"))) return;
        Object criteriaValue = signals.get("criteria");
        if (!(criteriaValue instanceof List<?> criteria)) return;
        Map<String, LearningContentClient.RubricCriterion> mappings = rubric.criteria().stream()
                .filter(item -> item.knowledgePointId() != null)
                .collect(java.util.stream.Collectors.toMap(LearningContentClient.RubricCriterion::code, item -> item,
                        (first, ignored) -> first));
        List<KnowledgeEvidence> additions = new ArrayList<>();
        for (Object value : criteria) {
            if (!(value instanceof Map<?, ?> row)) continue;
            Object codeValue = row.get("code");
            Object bandValue = row.get("band");
            if (!(codeValue instanceof String code) || !(bandValue instanceof Number number)) continue;
            LearningContentClient.RubricCriterion mapped = mappings.get(code);
            if (mapped == null) continue;
            BigDecimal band = new BigDecimal(number.toString());
            UUID sourceReference = UUID.nameUUIDFromBytes((submissionId + ":" + code).getBytes(StandardCharsets.UTF_8));
            additions.add(KnowledgeEvidence.of(mapped.knowledgePointId(), band.compareTo(passBand) >= 0,
                    EvidenceSource.SPEAKING_PRACTICE, sourceReference));
        }
        if (additions.isEmpty()) return;
        evidence.append(userId, additions);
        mastery.recordEvidence(userId, additions);
    }
}
