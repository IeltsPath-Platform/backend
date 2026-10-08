package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AttemptResponseResult;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** The saved responses of an owned attempt, with revisions, so a resumed attempt continues from the server state. */
@Service
public class ListAttemptResponsesUseCase {
    private final AssessmentAttemptRepository attempts;
    private final AttemptItemRepository items;
    private final AttemptResponseRepository responses;

    public ListAttemptResponsesUseCase(AssessmentAttemptRepository attempts, AttemptItemRepository items,
                                       AttemptResponseRepository responses) {
        this.attempts = attempts;
        this.items = items;
        this.responses = responses;
    }

    @Transactional(readOnly = true)
    public List<AttemptResponseResult> execute(UUID userId, UUID attemptId) {
        attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        List<UUID> itemIds = items.findByAttemptId(attemptId).stream().map(AttemptItem::id).toList();
        if (itemIds.isEmpty()) return List.of();
        return responses.findByAttemptItemIds(itemIds).stream()
                .map(r -> new AttemptResponseResult(r.id(), r.attemptItemId(), r.payload(), r.schemaVersion(),
                        r.revision(), r.savedAt(), r.submittedAt()))
                .toList();
    }
}
