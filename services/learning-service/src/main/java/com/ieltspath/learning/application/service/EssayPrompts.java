package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.EssayPrompt;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The snapshot an essay is graded against; chart facts and the sample answer stay server-side. */
public final class EssayPrompts {
    private EssayPrompts() {}

    /** Task 2 and Task 1 alike, from a lesson essay question with its chart images. */
    public static EssayPrompt of(LearningContentClient.Question question) {
        List<EssayPrompt.Image> images = question.assets() == null ? List.of() : question.assets().stream()
                .filter(asset -> "IMAGE".equals(asset.assetType()))
                .sorted(Comparator.comparingInt(LearningContentClient.QuestionAsset::sortOrder))
                .map(asset -> new EssayPrompt.Image(asset.mediaUrl(), asset.altText())).toList();
        return of(question.questionVersionId(), question.stem(), question.answerSpec(), question.explanation(), images,
                List.copyOf(question.knowledgePointIds()));
    }

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
