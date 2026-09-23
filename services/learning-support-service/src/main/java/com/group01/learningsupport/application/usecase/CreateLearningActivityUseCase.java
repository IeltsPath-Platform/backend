package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.CreateLearningActivityCommand;
import com.group01.learningsupport.domain.aggregate.LearningActivity;
import com.group01.learningsupport.domain.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateLearningActivityUseCase {
    private final LearningActivityRepository repository;

    @Transactional
    public LearningActivity execute(CreateLearningActivityCommand command) {
        return repository.save(LearningActivity.create(
                command.userId(),
                command.activityType(),
                command.sourceType(),
                command.sourceId(),
                command.occurredAt(),
                command.durationSeconds()
        ));
    }
}
