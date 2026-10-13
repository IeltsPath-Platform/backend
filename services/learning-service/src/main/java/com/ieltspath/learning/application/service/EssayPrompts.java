package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.EssayPrompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The snapshot an essay is graded against; chart facts and the sample answer stay server-side. */
public final class EssayPrompts {
    private EssayPrompts() {}

    /** From an essay item of a practice set; package items carry no images. */
    public static EssayPrompt of(LearningContentClient.Item item) {
        return of(item.questionVersionId(), item.stem(), item.answerSpec(), item.explanation(), List.of(),
                item.knowledgePointMappings().stream().map(LearningContentClient.KnowledgePointMapping::knowledgePointId)
                        .distinct().toList());
    }

    private static EssayPrompt of(UUID questionVersionId, String stem, Map<String, Object> answerSpec,
                                  String explanation, List<EssayPrompt.Image> images,
                                  List<UUID> knowledgePointIds) {
        Map<String, Object> spec = answerSpec == null ? Map.of() : answerSpec;
        return new EssayPrompt(questionVersionId, stem,
                spec.get("task") instanceof String task ? task : null,
                spec.get("minWords") instanceof Number words ? words.intValue() : null,
                spec.get("passBand") instanceof Number band ? new BigDecimal(band.toString()) : null,
                spec.get("chartFacts") instanceof String facts ? facts : null,
                explanation, images, knowledgePointIds);
    }
}
