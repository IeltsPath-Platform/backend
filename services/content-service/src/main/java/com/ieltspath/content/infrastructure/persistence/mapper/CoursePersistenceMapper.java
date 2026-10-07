package com.ieltspath.content.infrastructure.persistence.mapper;

import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.infrastructure.persistence.entity.CourseJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CoursePersistenceMapper {
    Course toDomain(CourseJpaEntity entity);
    CourseJpaEntity toEntity(Course course);
}
