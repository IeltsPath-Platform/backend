package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.command.CreateLearningActivityCommand;
import com.ieltspath.user.application.result.LearningActivityResult;
import com.ieltspath.user.domain.aggregate.LearningActivity;
import com.ieltspath.user.domain.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateLearningActivityUseCase {
    private final LearningActivityRepository repository;

    @Transactional
    public LearningActivityResult execute(CreateLearningActivityCommand command) {
        return LearningActivityResult.from(repository.save(LearningActivity.create(
                command.userId(),
                command.activityType(),
                command.sourceType(),
                command.sourceId(),
                command.occurredAt(),
                command.durationSeconds()
        )));
    }
}
