package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.CourseNotEmptyException;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeleteCourseUseCase {
    private final CourseRepository repository;

    public DeleteCourseUseCase(CourseRepository repository) { this.repository = repository; }

    /** Deletes a course that holds nothing yet; a course with content is stopped instead. */
    public void execute(UUID id) {
        repository.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        if (repository.hasContent(id)) throw new CourseNotEmptyException(id);
        repository.deleteById(id);
    }
}
