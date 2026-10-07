package com.group01.content.application.usecase;

import com.group01.content.application.result.CourseResult;
import com.group01.content.domain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListCoursesUseCase {
    private final CourseRepository repository;

    public ListCoursesUseCase(CourseRepository repository) { this.repository = repository; }

    public List<CourseResult> execute() {
        return repository.findAllOrderByBandLevel().stream().map(CourseResult::from).toList();
    }
}
