package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.Course;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(UUID id);
    Optional<Course> findByBandLevel(BigDecimal bandLevel);
    boolean existsByCode(String code);
    List<Course> findAllOrderByBandLevel();
}
