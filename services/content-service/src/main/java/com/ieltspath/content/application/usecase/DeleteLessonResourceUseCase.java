package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.LessonResourceNotFoundException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeleteLessonResourceUseCase {
    private final LessonResourceRepository resources;

    public DeleteLessonResourceUseCase(LessonResourceRepository resources) { this.resources = resources; }

    public void execute(UUID lessonId, UUID resourceId) {
        var resource = resources.findById(resourceId)
                .filter(found -> found.getLessonId().equals(lessonId))
                .orElseThrow(() -> new LessonResourceNotFoundException(lessonId, resourceId));
        resources.delete(resource);
    }
}
