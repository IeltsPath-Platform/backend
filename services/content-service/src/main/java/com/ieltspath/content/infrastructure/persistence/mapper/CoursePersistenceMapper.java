package com.ieltspath.content.infrastructure.persistence.mapper;

import com.ieltspath.content.domain.aggregate.Course;
import com.ieltspath.content.infrastructure.persistence.entity.CourseJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CoursePersistenceMapper {
    @ObjectFactory
    default Course createCourse(CourseJpaEntity entity) {
        return new Course(entity.getId(), entity.getCode(), entity.getName(), entity.getBandLevel(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCoverImageUrl(),
                entity.getDescription(), entity.getSkillUnlockPoints(), entity.getComboUnlockPoints());
    }

    @Mapping(target = "skillUnlockPoints", ignore = true)
    @Mapping(target = "comboUnlockPoints", ignore = true)
    Course toDomain(CourseJpaEntity entity);
    CourseJpaEntity toEntity(Course course);
}
