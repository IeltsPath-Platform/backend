package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AttemptStructureResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper json;

    public GetAttemptStructureUseCase(AssessmentAttemptRepository attempts,
                                      AttemptSectionRepository sections,
                                      AttemptItemRepository items,
                                      ObjectMapper json) {
        this.attempts = attempts;
        this.sections = sections;
        this.items = items;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public AttemptStructureResult execute(UUID userId, UUID attemptId) {
        attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));

        Map<UUID, List<AttemptItem>> grouped = new HashMap<>();
        items.findByAttemptId(attemptId).forEach(item ->
                grouped.computeIfAbsent(item.attemptSectionId(), ignored -> new ArrayList<>()).add(item));

        var values = sections.findByAttemptId(attemptId).stream()
                .map(section -> new AttemptStructureResult.Section(
                        section.id(),
                        section.contentSectionId(),
                        section.sortOrder(),
                        section.partNo(),
                        section.snapshot(),
                        grouped.getOrDefault(section.id(), List.of()).stream()
                                .map(item -> new AttemptStructureResult.Item(
                                        item.id(),
                                        item.questionVersionId(),
                                        item.sortOrder(),
                                        item.questionSnapshot(),
                                        item.knowledgeSnapshot(),
                                        answerSeconds(item.answerSnapshot())))
                                .toList(),
                        section.startedAt(),
                        section.completedAt()))
                .toList();
        return new AttemptStructureResult(values);
    }

    /** The one thing a learner may know from the answer snapshot before submitting: the length of a spoken answer. */
    private Integer answerSeconds(String answerSnapshot) {
        return AnswerSnapshot.parse(json, answerSnapshot)
                .map(AnswerSnapshot::answerSpec)
                .filter(spec -> "SPEAKING".equals(spec.get("type")))
                .map(spec -> spec.get("durationSeconds") instanceof Number seconds && seconds.intValue() > 0
                        ? seconds.intValue() : null)
                .orElse(null);
    }
}
