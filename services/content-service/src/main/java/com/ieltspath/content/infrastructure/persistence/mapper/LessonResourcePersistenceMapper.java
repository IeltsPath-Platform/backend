package com.ieltspath.content.infrastructure.persistence.mapper;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.infrastructure.persistence.entity.LessonResourceJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LessonResourcePersistenceMapper {
    LessonResource toDomain(LessonResourceJpaEntity entity);
    LessonResourceJpaEntity toEntity(LessonResource resource);
}
