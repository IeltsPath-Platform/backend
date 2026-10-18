package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateLessonResourceCommand;
import com.ieltspath.content.application.result.LessonResourceResult;
import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.LessonResourceNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateLessonResourceUseCase {
    private final LessonResourceRepository resources;

    public UpdateLessonResourceUseCase(LessonResourceRepository resources) { this.resources = resources; }

    public LessonResourceResult execute(UpdateLessonResourceCommand command) {
        LessonResource resource = resources.findById(command.resourceId())
                .filter(found -> found.getLessonId().equals(command.lessonId()))
                .orElseThrow(() -> new LessonResourceNotFoundException(command.lessonId(), command.resourceId()));
        resource.update(command.title(), command.url(), command.sortOrder());
        return LessonResourceResult.from(resources.save(resource));
    }
}
