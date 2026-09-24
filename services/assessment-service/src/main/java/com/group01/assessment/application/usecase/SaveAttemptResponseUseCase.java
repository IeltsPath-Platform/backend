package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.SaveAttemptResponseCommand;
import com.group01.assessment.application.result.AttemptResponseResult;
import com.group01.assessment.domain.entity.AttemptResponse;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.exception.RevisionConflictException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.domain.repository.AttemptResponseRepository;
import com.group01.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SaveAttemptResponseUseCase {

    private final AssessmentAttemptRepository attempts;
    private final AttemptItemRepository items;
    private final AttemptResponseRepository responses;

    public SaveAttemptResponseUseCase(
            AssessmentAttemptRepository attempts,
            AttemptItemRepository items,
            AttemptResponseRepository responses
    ) {
        this.attempts = attempts;
        this.items = items;
        this.responses = responses;
    }

    @Transactional
    public AttemptResponseResult execute(SaveAttemptResponseCommand command) {
        var attempt = attempts.findByIdAndUserId(command.attemptId(), command.userId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new InvalidAssessmentStateException("Only in-progress attempts accept responses");
        }

        items.findByIdAndAttemptId(command.itemId(), command.attemptId())
                .orElseThrow(() -> new AssessmentNotFoundException("Attempt item not found"));

        var oldResponse = responses.findByAttemptItemId(command.itemId());
        long currentRevision = oldResponse.map(AttemptResponse::revision).orElse(0L);
        if (currentRevision != command.expectedRevision()) {
            throw new RevisionConflictException("Response revision conflict");
        }

        var response = new AttemptResponse(
                oldResponse.map(AttemptResponse::id).orElse(UUID.randomUUID()),
                command.itemId(),
                command.payload(),
                command.schemaVersion(),
                currentRevision + 1,
                Instant.now(),
                null,
                oldResponse.map(AttemptResponse::lockVersion).orElse(0L)
        );
        AttemptResponse saved = responses.save(response);
        return new AttemptResponseResult(
                saved.id(),
                saved.attemptItemId(),
                saved.payload(),
                saved.schemaVersion(),
                saved.revision(),
                saved.savedAt(),
                saved.submittedAt()
        );
    }
}
