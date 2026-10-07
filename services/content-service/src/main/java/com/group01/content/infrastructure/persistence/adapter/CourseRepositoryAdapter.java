package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.domain.aggregate.Course;
import com.group01.content.domain.exception.DuplicateCourseException;
import com.group01.content.domain.repository.CourseRepository;
import com.group01.content.infrastructure.persistence.mapper.CoursePersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.CourseJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CourseRepositoryAdapter implements CourseRepository {
    private final CourseJpaRepository repository;
    private final CoursePersistenceMapper mapper;

    public CourseRepositoryAdapter(CourseJpaRepository repository, CoursePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Course save(Course course) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(course)));
        } catch (DataIntegrityViolationException ex) {
            // Translate only the course unique constraints; other persistence failures retain their cause.
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint) {
                    if ("courses_code_key".equals(constraint.getConstraintName())) {
                        throw new DuplicateCourseException("code", course.getCode());
                    }
                    if ("courses_band_level_key".equals(constraint.getConstraintName())) {
                        throw new DuplicateCourseException("bandLevel", course.getBandLevel().toPlainString());
                    }
                }
            }
            throw ex;
        }
    }

    @Override
    public Optional<Course> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }

    @Override
    public Optional<Course> findByBandLevel(BigDecimal bandLevel) { return repository.findByBandLevel(bandLevel).map(mapper::toDomain); }

    @Override
    public boolean existsByCode(String code) { return repository.existsByCode(code); }

    @Override
    public List<Course> findAllOrderByBandLevel() {
        return repository.findAllByOrderByBandLevelAsc().stream().map(mapper::toDomain).toList();
    }
}
