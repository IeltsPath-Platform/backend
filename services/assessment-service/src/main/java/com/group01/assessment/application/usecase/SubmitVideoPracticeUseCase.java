package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateVideoPracticeAttemptCommand;
import com.group01.assessment.application.result.VideoPracticeAttemptResult;
import com.group01.assessment.domain.entity.VideoPracticeAttempt;
import com.group01.assessment.domain.repository.VideoPracticeAttemptRepository;
import com.group01.assessment.domain.vo.PracticeStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SubmitVideoPracticeUseCase {
    private final VideoPracticeAttemptRepository repository;

    public SubmitVideoPracticeUseCase(VideoPracticeAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VideoPracticeAttemptResult execute(CreateVideoPracticeAttemptCommand command) {
        Instant now = Instant.now();
        var saved = repository.save(new VideoPracticeAttempt(
                UUID.randomUUID(), command.userId(), command.videoId(), command.segmentId(),
                command.practiceType(), command.referenceTextSnapshot(), null, null, null,
                PracticeStatus.IN_PROGRESS, now, null, now, null));
        return result(saved);
    }

    private VideoPracticeAttemptResult result(VideoPracticeAttempt attempt) {
        return new VideoPracticeAttemptResult(attempt.id(), attempt.userId(), attempt.videoId(),
                attempt.practiceType(), attempt.status(), attempt.startedAt(), attempt.completedAt(),
                attempt.resultPayload());
    }
}
