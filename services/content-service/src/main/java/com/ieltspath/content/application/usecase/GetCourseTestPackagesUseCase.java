package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.TopicTestPackageResult;
import com.ieltspath.content.domain.exception.CourseNotFoundException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCourseTestPackagesUseCase {
    private final LearningContentReader reader;
    private final CourseRepository courses;

    public GetCourseTestPackagesUseCase(LearningContentReader reader, CourseRepository courses) {
        this.reader = reader;
        this.courses = courses;
    }

    public List<TopicTestPackageResult> execute(UUID courseId) {
        courses.findById(courseId).filter(course -> course.getStatus() == ContentStatus.ACTIVE)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        return reader.courseTestPackages(courseId);
    }
}
