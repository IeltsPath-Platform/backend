package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AttemptStructureResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GetAttemptStructureUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AttemptSectionRepository sections;
    private final AttemptItemRepository items;

    public GetAttemptStructureUseCase(AssessmentAttemptRepository attempts,
                                      AttemptSectionRepository sections,
                                      AttemptItemRepository items) {
        this.attempts = attempts;
        this.sections = sections;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public AttemptStructureResult execute(UUID userId, UUID attemptId) {
        attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));

        Map<UUID, List<AttemptItem>> grouped = new HashMap<>();
        items.findByAttemptId(attemptId).forEach(item ->
                grouped.computeIfAbsent(item.attemptSectionId(), ignored -> new ArrayList<>()).add(item));

        var values = sections.findByAttemptId(attemptId).stream()
                .map(section -> new AttemptStructureResult.Section(
                        section.id(),
                        section.contentSectionId(),
                        section.sortOrder(),
                        section.snapshot(),
                        grouped.getOrDefault(section.id(), List.of()).stream()
                                .map(item -> new AttemptStructureResult.Item(
                                        item.id(),
                                        item.questionVersionId(),
                                        item.sortOrder(),
                                        item.questionSnapshot(),
                                        item.knowledgeSnapshot()))
                                .toList()))
                .toList();
        return new AttemptStructureResult(values);
    }
}
