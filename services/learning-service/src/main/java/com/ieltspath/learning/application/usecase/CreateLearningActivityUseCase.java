package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.CreateLearningActivityCommand;
import com.ieltspath.learning.application.result.LearningActivityResult;
import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.domain.repository.LearningActivityRepository;
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
