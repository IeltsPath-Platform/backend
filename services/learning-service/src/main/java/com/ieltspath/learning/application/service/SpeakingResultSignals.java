package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.SpeakingSubmission;
import com.ieltspath.learning.domain.service.SpeakingEvidenceRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads what the learning path may use from the {@code signals} layer of a stored Speaking result: whether the grade can be
 * trusted as evidence, the band and knowledge point of each criterion, and the error codes the grader marked. The stored
 * result is plain JSON, so every value is checked before use and a malformed entry is skipped.
 */
public final class SpeakingResultSignals {
    private SpeakingResultSignals() {
    }

    public static boolean usableAsEvidence(SpeakingSubmission answer) {
        return Boolean.TRUE.equals(signals(answer).get("usableAsEvidence"));
    }

    /** The criteria that carry a band; {@code knowledgePointId} is null when the rubric maps the criterion to no point. */
    public static List<SpeakingEvidenceRule.Criterion> criteria(SpeakingSubmission answer) {
        List<SpeakingEvidenceRule.Criterion> criteria = new ArrayList<>();
        if (!(signals(answer).get("criteria") instanceof List<?> rows)) return criteria;
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map) || !(map.get("code") instanceof String code)
                    || !(map.get("band") instanceof Number band)) continue;
            criteria.add(new SpeakingEvidenceRule.Criterion(code, uuid(map.get("kpId")), new BigDecimal(band.toString())));
        }
        return criteria;
    }

    /** The code of every marked error, repeated codes included. */
    public static List<String> errorCodes(SpeakingSubmission answer) {
        List<String> codes = new ArrayList<>();
        if (!(signals(answer).get("errors") instanceof List<?> rows)) return codes;
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map && map.get("code") instanceof String code && !code.isBlank()) codes.add(code);
        }
        return codes;
    }

    private static Map<?, ?> signals(SpeakingSubmission answer) {
        return answer.result() != null && answer.result().get("signals") instanceof Map<?, ?> signals ? signals : Map.of();
    }

    private static UUID uuid(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
