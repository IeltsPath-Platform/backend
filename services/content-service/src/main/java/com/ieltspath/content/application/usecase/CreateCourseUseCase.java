package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.CreateCourseCommand;
import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.DuplicateCourseException;
import com.ieltspath.content.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCourseUseCase {
    private final CourseRepository repository;

    public CreateCourseUseCase(CourseRepository repository) { this.repository = repository; }

    public CourseResult execute(CreateCourseCommand command) {
        Course course = Course.create(command.code(), command.name(), command.bandLevel());
        if (repository.existsByCode(course.getCode())) {
            throw new DuplicateCourseException("code", course.getCode());
        }
        if (repository.findByBandLevel(course.getBandLevel()).isPresent()) {
            throw new DuplicateCourseException("bandLevel", course.getBandLevel().toPlainString());
        }
        return CourseResult.from(repository.save(course));
    }
}
