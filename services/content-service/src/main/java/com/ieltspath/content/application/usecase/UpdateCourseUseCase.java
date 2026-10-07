package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdateCourseCommand;
import com.ieltspath.content.application.result.CourseResult;
import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.exception.DuplicateCourseException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.vo.BandRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCourseUseCase {
    private final CourseRepository repository;

    public UpdateCourseUseCase(CourseRepository repository) { this.repository = repository; }

    public CourseResult execute(UpdateCourseCommand command) {
        Course course = repository.findById(command.id()).orElseThrow(() -> new CourseNotFoundException(command.id()));
        if (command.bandLevel() == null) throw new IllegalArgumentException("bandLevel is required");
        var band = BandRange.of(command.bandLevel(), command.bandLevel()).min();
        if (repository.findByBandLevel(band).filter(existing -> !existing.getId().equals(course.getId())).isPresent()) {
            throw new DuplicateCourseException("bandLevel", band.toPlainString());
        }
        course.update(command.name(), band, command.status());
        return CourseResult.from(repository.save(course));
    }
}
