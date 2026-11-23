package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.QuestionPreviewKnowledgePointResult;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adds authoring-only question metadata in batches when the caller requests it. */
@Service
@Transactional(readOnly = true)
public class ListQuestionsWithIncludesUseCase {
    private final ListQuestionsUseCase listQuestions;
    private final QuestionRepository questions;
    private final LessonAuthoringRepository lessonAuthoring;

    public ListQuestionsWithIncludesUseCase(ListQuestionsUseCase listQuestions, QuestionRepository questions,
                                            LessonAuthoringRepository lessonAuthoring) {
        this.listQuestions = listQuestions;
        this.questions = questions;
        this.lessonAuthoring = lessonAuthoring;
    }

    public ListQuestionsUseCase.Page execute(Skill skill, QuestionPurpose purpose, PublicationStatus status,
                                             String code, int page, int size, Set<String> includes,
                                             boolean onlyFree, UUID exceptBlockId) {
        ListQuestionsUseCase.Page result = onlyFree
                ? listQuestions.executeOnlyFree(skill, purpose, status, code, exceptBlockId, page, size)
                : listQuestions.execute(skill, purpose, status, code, page, size);
        if (includes.isEmpty() || result.items().isEmpty()) return result;

        List<UUID> versionIds = result.items().stream().map(QuestionResult::currentPublishedVersionId)
                .filter(id -> id != null).distinct().toList();
        if (versionIds.isEmpty()) {
            return new ListQuestionsUseCase.Page(result.totalElements(), result.page(), result.size(),
                    result.items().stream().map(item -> item.enriched(
                            includes.contains("ownership") ? false : null,
                            null,
                            includes.contains("preview") ? List.of() : null,
                            null)).toList());
        }

        Set<UUID> owned = includes.contains("ownership")
                ? new HashSet<>(lessonAuthoring.questionVersionsOwnedElsewhere(exceptBlockId, versionIds))
                : Set.of();
        Map<UUID, String> excerpts = Map.of();
        Map<UUID, List<QuestionPreviewKnowledgePointResult>> knowledgePoints = Map.of();
        Map<UUID, Integer> totals = Map.of();
        if (includes.contains("preview")) {
            Map<UUID, String> stemExcerpts = new HashMap<>();
            questions.findVersionStems(versionIds).forEach(stem -> stemExcerpts.put(stem.versionId(), excerpt(stem.stem())));
            excerpts = stemExcerpts;
            Map<UUID, List<QuestionRepository.VersionKnowledgePoint>> grouped = questions
                    .findVersionKnowledgePoints(versionIds).stream()
                    .collect(Collectors.groupingBy(QuestionRepository.VersionKnowledgePoint::versionId));
            Map<UUID, List<QuestionPreviewKnowledgePointResult>> points = new HashMap<>();
            Map<UUID, Integer> pointTotals = new HashMap<>();
            grouped.forEach((versionId, rows) -> {
                points.put(versionId, rows.stream().map(row -> new QuestionPreviewKnowledgePointResult(row.code(), row.name())).toList());
                int total = rows.getFirst().total();
                if (total > 4) pointTotals.put(versionId, total);
            });
            knowledgePoints = points;
            totals = pointTotals;
        }

        Map<UUID, String> finalExcerpts = excerpts;
        Map<UUID, List<QuestionPreviewKnowledgePointResult>> finalKnowledgePoints = knowledgePoints;
        Map<UUID, Integer> finalTotals = totals;
        List<QuestionResult> enriched = result.items().stream().map(item -> {
            UUID versionId = item.currentPublishedVersionId();
            return item.enriched(
                    includes.contains("ownership") ? owned.contains(versionId) : null,
                    includes.contains("preview") ? finalExcerpts.get(versionId) : null,
                    includes.contains("preview") ? finalKnowledgePoints.getOrDefault(versionId, List.of()) : null,
                    includes.contains("preview") ? finalTotals.get(versionId) : null);
        }).toList();
        return new ListQuestionsUseCase.Page(result.totalElements(), result.page(), result.size(), enriched);
    }

    private static String excerpt(String stem) {
        if (stem == null || stem.length() <= 160) return stem;
        int end = 159;
        int boundary = stem.substring(0, end).lastIndexOf(' ');
        if (boundary > 0) end = boundary;
        return stem.substring(0, end).stripTrailing() + "…";
    }
}
