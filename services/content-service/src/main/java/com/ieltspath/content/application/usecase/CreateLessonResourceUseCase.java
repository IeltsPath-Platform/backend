package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateLessonResourceCommand;
import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateLessonResourceUseCase {
    private final LessonResourceRepository resources;
    private final LearningContentReader lessons;

    public CreateLessonResourceUseCase(LessonResourceRepository resources, LearningContentReader lessons) {
        this.resources = resources;
        this.lessons = lessons;
    }

    public LessonResourceResult execute(CreateLessonResourceCommand command) {
        LessonResource resource = LessonResource.create(command.lessonId(), command.resourceType(), command.title(),
                command.url(), command.sortOrder());
        if (!lessons.lessonExists(command.lessonId())) throw new LessonNotFoundException(command.lessonId());
        return LessonResourceResult.from(resources.save(resource));
    }
}
